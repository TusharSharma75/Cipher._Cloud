package com.ciphercloudx.controller;

import com.ciphercloudx.dto.*;
import com.ciphercloudx.security.UserPrincipal;
import com.ciphercloudx.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponseDto<String>> signup(@Valid @RequestBody SignupRequestDto request,
                                                         HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.signup(request, httpRequest));
    }
    @PostMapping("/forgot-password-otp")
    public ResponseEntity<ApiResponseDto<String>> sendResetOtp(
            @Valid @RequestBody ForgotPasswordOtpRequestDto request) {

        return ResponseEntity.ok(authService.sendResetOtp(request));
    }
    @PostMapping("/reset-password-otp")
    public ResponseEntity<ApiResponseDto<String>> resetPasswordOtp(
            @Valid @RequestBody ResetPasswordOtpRequestDto request) {

        return ResponseEntity.ok(authService.resetPasswordWithOtp(request));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request,
                                                                 HttpServletRequest httpRequest) {
        AuthResponseDto response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponseDto.success(response, "Login successful"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> verifyOtp(@Valid @RequestBody OtpVerifyRequestDto request,
                                                                     HttpServletRequest httpRequest) {
        AuthResponseDto response = authService.verifyOtp(request, httpRequest);
        return ResponseEntity.ok(ApiResponseDto.success(response, "OTP verified"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<AuthResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        AuthResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponseDto.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponseDto<String>> logout(@RequestHeader("Authorization") String authHeader,
                                                         HttpServletRequest httpRequest) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        authService.logout(token, httpRequest);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Logged out successfully"));
    }

    @PostMapping("/2fa/enable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<String>> enableTwoFactor(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        authService.enableTwoFactor(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(null, "Two-factor authentication enabled"));
    }

    @PostMapping("/2fa/disable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<String>> disableTwoFactor(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        authService.disableTwoFactor(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(null, "Two-factor authentication disabled"));
    }
}
