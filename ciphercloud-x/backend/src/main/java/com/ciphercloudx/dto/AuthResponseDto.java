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
public class AuthResponseDto {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private boolean requiresOtp;
    private String otpSessionId;
    
    // User info
    private Long userId;
    private String username;
    private String email;
    private Role role;
    private boolean otpEnabled;
    private LocalDateTime lastLogin;
    
    // Storage info
    private Long storageQuota;
    private Long usedStorage;
    private Double storageUsagePercentage;
}
