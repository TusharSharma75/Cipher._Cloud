package com.ciphercloudx.entity;

import com.ciphercloudx.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_user_action", columnList = "user_id, action"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_ip_address", columnList = "ip_address")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 FIXED HERE (LAZY → EAGER)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionType action;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "details", length = 4000)
    private String details;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "request_duration_ms")
    private Long requestDurationMs;

    public static ActivityLog success(User user, ActionType action, String ipAddress, String deviceInfo) {
        return ActivityLog.builder()
                .user(user)
                .action(action)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .status("SUCCESS")
                .build();
    }

    public static ActivityLog failure(User user, ActionType action, String ipAddress,
                                      String deviceInfo, String errorMessage) {
        return ActivityLog.builder()
                .user(user)
                .action(action)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .status("FAILURE")
                .errorMessage(errorMessage)
                .build();
    }

    public static ActivityLog withFile(User user, ActionType action, Long fileId,
                                       String fileName, String ipAddress, String deviceInfo) {
        return ActivityLog.builder()
                .user(user)
                .action(action)
                .fileId(fileId)
                .fileName(fileName)
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .status("SUCCESS")
                .build();
    }
}
