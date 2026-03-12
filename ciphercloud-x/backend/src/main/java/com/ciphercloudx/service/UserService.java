package com.ciphercloudx.service;

import com.ciphercloudx.dto.*;
import com.ciphercloudx.entity.User;
import com.ciphercloudx.enums.ActionType;
import com.ciphercloudx.enums.Role;
import com.ciphercloudx.exception.AuthenticationException;
import com.ciphercloudx.repository.ActivityLogRepository;
import com.ciphercloudx.repository.FileMetadataRepository;
import com.ciphercloudx.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, 
                       FileMetadataRepository fileMetadataRepository,
                       ActivityLogRepository activityLogRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getCurrentUser(Long userId) {
        return getUserById(userId);
    }

    @Transactional
    public UserResponseDto updateUser(Long userId, UserUpdateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Update email if provided
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new AuthenticationException("Email is already registered");
            }
            user.setEmail(request.getEmail());
        }

        // Update password if provided
        if (request.getNewPassword() != null) {
            if (request.getCurrentPassword() == null) {
                throw new AuthenticationException("Current password is required");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new AuthenticationException("Current password is incorrect");
            }
            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw new AuthenticationException("New passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // Update 2FA if provided
        if (request.getOtpEnabled() != null) {
            user.setOtpEnabled(request.getOtpEnabled());
        }

        userRepository.save(user);
        log.info("User updated: {}", user.getUsername());

        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        
        // Delete all user files
        fileMetadataRepository.findByOwnerIdAndNotDeleted(userId).forEach(file -> {
            fileMetadataRepository.delete(file);
        });
        
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }

    @Transactional
    public void changeUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));
        
        user.setRole(newRole);
        userRepository.save(user);
        log.info("User role changed: {} to {}", user.getUsername(), newRole);
    }

    private UserResponseDto mapToUserResponse(User user) {
        Long fileCount = fileMetadataRepository.countByOwnerId(user.getId());
        Long totalUploads = activityLogRepository.countByUserIdAndAction(user.getId(), ActionType.UPLOAD);
        Long totalDownloads = activityLogRepository.countByUserIdAndAction(user.getId(), ActionType.DOWNLOAD);

        double storageUsagePercentage = user.getStorageQuota() > 0 
                ? (double) user.getUsedStorage() / user.getStorageQuota() * 100 
                : 0;

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .otpEnabled(user.isOtpEnabled())
                .emailVerified(user.isEmailVerified())
                .accountLocked(user.isAccountLocked())
                .failedAttempts(user.getFailedAttempts())
                .lockTime(user.getLockTime())
                .lastLogin(user.getLastLogin())
                .storageQuota(user.getStorageQuota())
                .usedStorage(user.getUsedStorage())
                .storageUsagePercentage(Math.round(storageUsagePercentage * 100.0) / 100.0)
                .formattedStorageQuota(formatFileSize(user.getStorageQuota()))
                .formattedUsedStorage(formatFileSize(user.getUsedStorage()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .fileCount(fileCount)
                .totalUploads(totalUploads)
                .totalDownloads(totalDownloads)
                .build();
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
