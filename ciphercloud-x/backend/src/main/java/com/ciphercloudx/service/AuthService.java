package com.ciphercloudx.service;

import com.ciphercloudx.audit.AuditService;
import com.ciphercloudx.dto.*;
import com.ciphercloudx.entity.User;
import com.ciphercloudx.enums.ActionType;
import com.ciphercloudx.enums.Role;
import com.ciphercloudx.exception.AuthenticationException;
import com.ciphercloudx.repository.UserRepository;
import com.ciphercloudx.security.JwtTokenProvider;
import com.ciphercloudx.security.UserPrincipal;
import com.ciphercloudx.encryption.EncryptionService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;
    private final EmailService emailService;
    private final EncryptionService encryptionService;

    @Value("${security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.lock-duration-minutes:30}")
    private int lockDurationMinutes;

    private final Map<String, OtpSession> otpSessions = new ConcurrentHashMap<>();

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       AuditService auditService,
                       EmailService emailService,
                       EncryptionService encryptionService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
        this.emailService = emailService;
        this.encryptionService = encryptionService;
    }

    // ========================
    // SIGNUP
    // ========================

    @Transactional
    public ApiResponseDto<String> signup(SignupRequestDto request, HttpServletRequest httpRequest) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("Passwords do not match");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthenticationException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("Email is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .storageQuota(1073741824L)
                .usedStorage(0L)
                .accountLocked(false)
                .failedAttempts(0)
                .otpEnabled(false)
                .emailVerified(false)
                .build();

        userRepository.save(user);

        auditService.logActivity(user, ActionType.LOGIN, httpRequest);

        return ApiResponseDto.success("User registered successfully");
    }

    // ========================
    // LOGIN
    // ========================

    @Transactional
    public AuthResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest) {

        User user = userRepository
                .findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isAccountLocked()) {

            if (user.getLockTime() != null &&
                    ChronoUnit.MINUTES.between(user.getLockTime(), LocalDateTime.now()) >= lockDurationMinutes) {

                user.unlockAccount();
                userRepository.save(user);

            } else {

                auditService.logActivityFailure(user, ActionType.LOGIN, "Account locked", httpRequest);

                throw new LockedException("Account is locked.");
            }
        }

        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            user.resetFailedAttempts();
            userRepository.save(user);

            if (user.isOtpEnabled()) {

                String otpSessionId = generateOtpSession(user);

                return AuthResponseDto.builder()
                        .requiresOtp(true)
                        .otpSessionId(otpSessionId)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .otpEnabled(true)
                        .build();
            }

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            String accessToken = jwtTokenProvider.generateAccessToken(principal);
            String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            auditService.logActivity(user, ActionType.LOGIN, httpRequest);

            return buildAuthResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException e) {

            user.incrementFailedAttempts();

            if (user.getFailedAttempts() >= maxFailedAttempts) {

                user.lockAccount();
                userRepository.save(user);

                throw new LockedException("Account locked due to too many failed attempts.");
            }

            userRepository.save(user);

            throw new BadCredentialsException("Invalid credentials");
        }
    }

    // ========================
    // VERIFY OTP
    // ========================

    @Transactional
    public AuthResponseDto verifyOtp(OtpVerifyRequestDto request, HttpServletRequest httpRequest) {

        OtpSession session = otpSessions.get(request.getOtpSessionId());

        if (session == null) {
            throw new AuthenticationException("Invalid OTP session");
        }

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!session.getOtpCode().equals(request.getOtpCode())
                || session.getExpiryTime().isBefore(LocalDateTime.now())) {

            throw new AuthenticationException("Invalid or expired OTP");
        }

        otpSessions.remove(request.getOtpSessionId());

        UserPrincipal principal = UserPrincipal.create(user);

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        user.setLastLogin(LocalDateTime.now());
        user.setTwoFactorVerified(true);

        userRepository.save(user);

        auditService.logActivity(user, ActionType.LOGIN, httpRequest);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ========================
    // REFRESH TOKEN
    // ========================

    @Transactional
    public AuthResponseDto refreshToken(RefreshTokenRequestDto request) {

        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new AuthenticationException("Invalid refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        UserPrincipal principal = UserPrincipal.create(user);

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ========================
    // LOGOUT
    // ========================

    @Transactional
    public void logout(String token, HttpServletRequest httpRequest) {

        if (token != null && !token.isEmpty()) {

            jwtTokenProvider.blacklistToken(token);

            try {

                Long userId = jwtTokenProvider.getUserIdFromToken(token);

                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    auditService.logActivity(user, ActionType.LOGOUT, httpRequest);
                }

            } catch (Exception e) {
                log.warn("Logout audit failed", e);
            }
        }
    }

    // ========================
    // ENABLE 2FA
    // ========================

    @Transactional
    public void enableTwoFactor(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        String otpSecret = encryptionService.generateOtpSecret();
        String[] backupCodes = encryptionService.generateBackupCodes(10);

        user.setOtpEnabled(true);
        user.setOtpSecret(otpSecret);

        user.setBackupCodes(new ArrayList<>(Arrays.asList(backupCodes)));

        userRepository.save(user);
    }
    @Transactional
    public ApiResponseDto<String> sendResetOtp(ForgotPasswordOtpRequestDto request) {

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if(userOptional.isEmpty()) {
            return ApiResponseDto.success("If email exists OTP has been sent.");
        }

        User user = userOptional.get();

        String otp = encryptionService.generateNumericCode(6);

        user.setPasswordResetOtp(otp);
        user.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);

        return ApiResponseDto.success("OTP sent to email");
    }

    @Transactional
    public ApiResponseDto<String> resetPasswordWithOtp(ResetPasswordOtpRequestDto request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if(!request.getOtp().equals(user.getPasswordResetOtp())) {
            throw new AuthenticationException("Invalid OTP");
        }

        if(user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("OTP expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        user.setPasswordResetOtp(null);
        user.setPasswordResetExpiry(null);

        userRepository.save(user);

        return ApiResponseDto.success("Password reset successful");
    }

    // ========================
    // DISABLE 2FA
    // ========================

    @Transactional
    public void disableTwoFactor(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        user.setOtpEnabled(false);
        user.setOtpSecret(null);
        user.setBackupCodes(new ArrayList<>());

        userRepository.save(user);
    }

    private String generateOtpSession(User user) {

        String sessionId = UUID.randomUUID().toString();
        String otpCode = encryptionService.generateNumericCode(6);

        OtpSession session = new OtpSession();

        session.setUserId(user.getId());
        session.setOtpCode(otpCode);
        session.setExpiryTime(LocalDateTime.now().plusMinutes(5));

        otpSessions.put(sessionId, session);

        emailService.sendOtpEmail(user.getEmail(), otpCode);

        return sessionId;
    }

    private AuthResponseDto buildAuthResponse(User user, String accessToken, String refreshToken) {

        double usage = user.getStorageQuota() > 0
                ? (double) user.getUsedStorage() / user.getStorageQuota() * 100
                : 0;

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .requiresOtp(false)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .otpEnabled(user.isOtpEnabled())
                .lastLogin(user.getLastLogin())
                .storageQuota(user.getStorageQuota())
                .usedStorage(user.getUsedStorage())
                .storageUsagePercentage(Math.round(usage * 100.0) / 100.0)
                .build();
    }

    private static class OtpSession {

        private Long userId;
        private String otpCode;
        private LocalDateTime expiryTime;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

        public LocalDateTime getExpiryTime() { return expiryTime; }
        public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
    }
}