package com.ciphercloudx.controller;

import com.ciphercloudx.audit.AuditService;
import com.ciphercloudx.dto.ActivityLogResponseDto;
import com.ciphercloudx.dto.ApiResponseDto;
import com.ciphercloudx.entity.ActivityLog;
import com.ciphercloudx.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activity-logs")
@PreAuthorize("isAuthenticated()")
public class ActivityLogController {

    private final AuditService auditService;

    @Autowired
    public ActivityLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponseDto<Page<ActivityLogResponseDto>>> getMyActivityLogs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Pageable pageable) {
        
        Page<ActivityLog> logs = auditService.getUserActivity(userPrincipal.getId(), pageable);
        Page<ActivityLogResponseDto> response = logs.map(this::mapToDto);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Page<ActivityLogResponseDto>>> getAllActivityLogs(Pageable pageable) {
        Page<ActivityLog> logs = auditService.getAllActivity(pageable);
        Page<ActivityLogResponseDto> response = logs.map(this::mapToDto);
        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    private ActivityLogResponseDto mapToDto(ActivityLog log) {
        return ActivityLogResponseDto.builder()
                .id(log.getId())
                .userId(log.getUser().getId())
                .username(log.getUser().getUsername())
                .action(log.getAction())
                .actionDisplayName(log.getAction().name())
                .fileId(log.getFileId())
                .fileName(log.getFileName())
                .timestamp(log.getTimestamp())
                .ipAddress(log.getIpAddress())
                .deviceInfo(log.getDeviceInfo())
                .userAgent(log.getUserAgent())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .details(log.getDetails())
                .requestDurationMs(log.getRequestDurationMs())
                .build();
    }
}
