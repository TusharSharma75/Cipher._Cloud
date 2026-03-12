package com.ciphercloudx.audit;

import com.ciphercloudx.entity.ActivityLog;
import com.ciphercloudx.entity.User;
import com.ciphercloudx.enums.ActionType;
import com.ciphercloudx.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class AuditService {

    private final ActivityLogRepository activityLogRepository;

    @Autowired
    public AuditService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Async
    public void logActivity(User user, ActionType action, HttpServletRequest request) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .user(user)
                    .action(action)
                    .ipAddress(getClientIpAddress(request))
                    .deviceInfo(getDeviceInfo(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .status("SUCCESS")
                    .build();
            
            activityLogRepository.save(activityLog);
            log.debug("Logged activity: {} for user: {}", action, user.getUsername());
        } catch (Exception e) {
            log.error("Failed to log activity", e);
        }
    }

    @Async
    public void logActivity(User user, ActionType action, Long fileId, String fileName, 
                            HttpServletRequest request) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .user(user)
                    .action(action)
                    .fileId(fileId)
                    .fileName(fileName)
                    .ipAddress(getClientIpAddress(request))
                    .deviceInfo(getDeviceInfo(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .status("SUCCESS")
                    .build();
            
            activityLogRepository.save(activityLog);
            log.debug("Logged activity: {} for user: {} file: {}", action, user.getUsername(), fileName);
        } catch (Exception e) {
            log.error("Failed to log activity", e);
        }
    }

    @Async
    public void logActivityFailure(User user, ActionType action, String errorMessage, 
                                   HttpServletRequest request) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .user(user)
                    .action(action)
                    .ipAddress(getClientIpAddress(request))
                    .deviceInfo(getDeviceInfo(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .status("FAILURE")
                    .errorMessage(errorMessage)
                    .build();
            
            activityLogRepository.save(activityLog);
            log.debug("Logged failed activity: {} for user: {} error: {}", action, user.getUsername(), errorMessage);
        } catch (Exception e) {
            log.error("Failed to log activity failure", e);
        }
    }

    @Async
    public void logActivityWithDetails(User user, ActionType action, String details, 
                                       HttpServletRequest request, long durationMs) {
        try {
            ActivityLog activityLog = ActivityLog.builder()
                    .user(user)
                    .action(action)
                    .ipAddress(getClientIpAddress(request))
                    .deviceInfo(getDeviceInfo(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .status("SUCCESS")
                    .details(details)
                    .requestDurationMs(durationMs)
                    .build();
            
            activityLogRepository.save(activityLog);
        } catch (Exception e) {
            log.error("Failed to log activity with details", e);
        }
    }

    public Page<ActivityLog> getUserActivity(Long userId, Pageable pageable) {
        return activityLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    public Page<ActivityLog> getAllActivity(Pageable pageable) {
        return activityLogRepository.findAllOrderByTimestampDesc(pageable);
    }

    public List<ActivityLog> getRecentActivity(LocalDateTime since, Pageable pageable) {
        return activityLogRepository.findRecentActivity(since, pageable);
    }

    public Long countByActionSince(ActionType action, LocalDateTime since) {
        return activityLogRepository.countByActionSince(action, since);
    }

    public Long countFailedLoginsSince(LocalDateTime since) {
        return activityLogRepository.countFailedLoginsSince(since);
    }

    public Long countQuotaViolations() {
        return activityLogRepository.countQuotaViolations();
    }

    public Long countByUserIdAndAction(Long userId, ActionType action) {
        return activityLogRepository.countByUserIdAndAction(userId, action);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }
        
        // Simple device detection
        if (userAgent.contains("Mobile")) {
            return "Mobile";
        } else if (userAgent.contains("Tablet")) {
            return "Tablet";
        } else if (userAgent.contains("Windows") || userAgent.contains("Mac") || userAgent.contains("Linux")) {
            return "Desktop";
        }
        
        return "Unknown";
    }
}
