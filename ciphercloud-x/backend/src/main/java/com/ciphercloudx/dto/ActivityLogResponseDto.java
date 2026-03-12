package com.ciphercloudx.dto;

import com.ciphercloudx.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponseDto {
    
    private Long id;
    private Long userId;
    private String username;
    private ActionType action;
    private String actionDisplayName;
    private Long fileId;
    private String fileName;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String deviceInfo;
    private String userAgent;
    private String status;
    private String errorMessage;
    private String details;
    private Long requestDurationMs;
}
