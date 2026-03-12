package com.ciphercloudx.dto;

import com.ciphercloudx.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean otpEnabled;
    private boolean emailVerified;
    private boolean accountLocked;
    private Integer failedAttempts;
    private LocalDateTime lockTime;
    private LocalDateTime lastLogin;
    
    private Long storageQuota;
    private Long usedStorage;
    private Double storageUsagePercentage;
    private String formattedStorageQuota;
    private String formattedUsedStorage;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Long fileCount;
    private Long totalUploads;
    private Long totalDownloads;
}
