package com.ciphercloudx.analytics;

import com.ciphercloudx.dto.*;
import com.ciphercloudx.enums.ActionType;
import com.ciphercloudx.enums.IntegrityStatus;
import com.ciphercloudx.repository.ActivityLogRepository;
import com.ciphercloudx.repository.FileMetadataRepository;
import com.ciphercloudx.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalyticsService {

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final ActivityLogRepository activityLogRepository;

    @Autowired
    public AnalyticsService(UserRepository userRepository,
                            FileMetadataRepository fileMetadataRepository,
                            ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsDashboardDto getDashboardAnalytics() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);

        AnalyticsDashboardDto dashboard = new AnalyticsDashboardDto();

        // User statistics
        dashboard.setTotalUsers(userRepository.count());
        dashboard.setActiveUsers(userRepository.countActiveUsersSince(weekAgo));
        dashboard.setNewUsersToday(userRepository.countNewUsersSince(today));
        dashboard.setNewUsersThisWeek(userRepository.countNewUsersSince(weekAgo));
        dashboard.setNewUsersThisMonth(userRepository.countNewUsersSince(monthAgo));
        dashboard.setLockedAccounts(userRepository.countLockedAccounts());
        dashboard.setOtpEnabledUsers(userRepository.countOtpEnabledUsers());

        // Storage statistics
        Long totalStorageUsed = userRepository.sumUsedStorage();
        Long totalStorageQuota = userRepository.sumStorageQuota();
        
        dashboard.setTotalStorageUsed(totalStorageUsed != null ? totalStorageUsed : 0);
        dashboard.setFormattedTotalStorageUsed(formatFileSize(totalStorageUsed != null ? totalStorageUsed : 0));
        dashboard.setTotalStorageQuota(totalStorageQuota != null ? totalStorageQuota : 0);
        dashboard.setFormattedTotalStorageQuota(formatFileSize(totalStorageQuota != null ? totalStorageQuota : 0));
        dashboard.setOverallStorageUsagePercentage(totalStorageQuota != null && totalStorageQuota > 0 
                ? Math.round((double) totalStorageUsed / totalStorageQuota * 10000.0) / 100.0 
                : 0);
        dashboard.setTotalFiles(fileMetadataRepository.countAllActiveFiles());
        dashboard.setTotalDeletedFiles(fileMetadataRepository.countAllDeletedFiles());

        // Activity statistics
        dashboard.setUploadsToday(activityLogRepository.countByActionSince(ActionType.UPLOAD, today));
        dashboard.setDownloadsToday(activityLogRepository.countByActionSince(ActionType.DOWNLOAD, today));
        dashboard.setLoginsToday(activityLogRepository.countByActionSince(ActionType.LOGIN, today));
        dashboard.setFailedLoginsToday(activityLogRepository.countFailedLoginsSince(today));
        dashboard.setUploadsThisWeek(activityLogRepository.countByActionSince(ActionType.UPLOAD, weekAgo));
        dashboard.setDownloadsThisWeek(activityLogRepository.countByActionSince(ActionType.DOWNLOAD, weekAgo));
        dashboard.setUploadsThisMonth(activityLogRepository.countByActionSince(ActionType.UPLOAD, monthAgo));
        dashboard.setDownloadsThisMonth(activityLogRepository.countByActionSince(ActionType.DOWNLOAD, monthAgo));

        // File type distribution
        dashboard.setFileTypeDistribution(getFileTypeDistribution());
        dashboard.setFileTypeStats(getFileTypeStats());

        // Upload trends
        dashboard.setUploadTrends(getUploadTrends(30));
        dashboard.setDownloadTrends(getDownloadTrends(30));
        dashboard.setLoginTrends(getLoginTrends(30));

        // Top users
        dashboard.setTopUsersByStorage(getTopUsersByStorage(10));
        dashboard.setTopUsersByUploads(getTopUsersByUploads(10));

        // Recent activity
        dashboard.setRecentActivity(getRecentActivity(20));

        // Security metrics
        dashboard.setIntegrityFailures(fileMetadataRepository.countByIntegrityStatus(IntegrityStatus.FAILED));
        dashboard.setQuotaViolations(activityLogRepository.countQuotaViolations());
        dashboard.setSuspiciousActivities(activityLogRepository.countFailedLoginsSince(weekAgo));

        return dashboard;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getFileTypeDistribution() {
        List<Object[]> results = fileMetadataRepository.getFileTypeDistribution();
        Map<String, Long> distribution = new HashMap<>();
        
        for (Object[] result : results) {
            String contentType = (String) result[0];
            Long count = (Long) result[1];
            
            // Simplify content type
            String simplifiedType = simplifyContentType(contentType);
            distribution.merge(simplifiedType, count, Long::sum);
        }
        
        return distribution;
    }

    @Transactional(readOnly = true)
    public List<FileTypeStatDto> getFileTypeStats() {
        List<Object[]> results = fileMetadataRepository.getFileTypeDistribution();
        Long totalFiles = fileMetadataRepository.countAllActiveFiles();
        
        Map<String, FileTypeStatDto> statsMap = new HashMap<>();
        
        for (Object[] result : results) {
            String contentType = (String) result[0];
            Long count = (Long) result[1];
            Long totalSize = (Long) result[2];
            
            String simplifiedType = simplifyContentType(contentType);
            
            FileTypeStatDto existing = statsMap.get(simplifiedType);
            if (existing != null) {
                existing.setCount(existing.getCount() + count);
                existing.setTotalSize(existing.getTotalSize() + totalSize);
            } else {
                statsMap.put(simplifiedType, FileTypeStatDto.builder()
                        .fileType(simplifiedType)
                        .count(count)
                        .totalSize(totalSize)
                        .formattedTotalSize(formatFileSize(totalSize))
                        .percentage(totalFiles > 0 ? Math.round((double) count / totalFiles * 10000.0) / 100.0 : 0)
                        .build());
            }
        }
        
        return statsMap.values().stream()
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DailyActivityDto> getUploadTrends(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = activityLogRepository.countDailyWithSizeByActionSince(ActionType.UPLOAD, since);
        
        return results.stream()
                .map(r -> DailyActivityDto.builder()
                        .date(((java.sql.Date) r[0]).toLocalDate())
                        .count((Long) r[1])
                        .totalSize(r[2] != null ? (Long) r[2] : 0)
                        .formattedTotalSize(formatFileSize(r[2] != null ? (Long) r[2] : 0))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DailyActivityDto> getDownloadTrends(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = activityLogRepository.countDailyByActionSince(ActionType.DOWNLOAD, since);
        
        return results.stream()
                .map(r -> DailyActivityDto.builder()
                        .date(((java.sql.Date) r[0]).toLocalDate())
                        .count((Long) r[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DailyActivityDto> getLoginTrends(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = activityLogRepository.countDailyByActionSince(ActionType.LOGIN, since);
        
        return results.stream()
                .map(r -> DailyActivityDto.builder()
                        .date(((java.sql.Date) r[0]).toLocalDate())
                        .count((Long) r[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopUserDto> getTopUsersByStorage(int limit) {
        return userRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getUsedStorage(), a.getUsedStorage()))
                .limit(limit)
                .map(u -> TopUserDto.builder()
                        .userId(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .value(u.getUsedStorage())
                        .formattedValue(formatFileSize(u.getUsedStorage()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopUserDto> getTopUsersByUploads(int limit) {
        return userRepository.findAll().stream()
                .map(u -> {
                    Long uploadCount = activityLogRepository.countByUserIdAndAction(u.getId(), ActionType.UPLOAD);
                    return TopUserDto.builder()
                            .userId(u.getId())
                            .username(u.getUsername())
                            .email(u.getEmail())
                            .value(uploadCount)
                            .formattedValue(uploadCount.toString())
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityLogResponseDto> getRecentActivity(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return activityLogRepository.findRecentActivity(since, PageRequest.of(0, limit))
                .stream()
                .map(a -> ActivityLogResponseDto.builder()
                        .id(a.getId())
                        .userId(a.getUser().getId())
                        .username(a.getUser().getUsername())
                        .action(a.getAction())
                        .actionDisplayName(a.getAction().name())
                        .fileId(a.getFileId())
                        .fileName(a.getFileName())
                        .timestamp(a.getTimestamp())
                        .ipAddress(a.getIpAddress())
                        .deviceInfo(a.getDeviceInfo())
                        .userAgent(a.getUserAgent())
                        .status(a.getStatus())
                        .errorMessage(a.getErrorMessage())
                        .details(a.getDetails())
                        .requestDurationMs(a.getRequestDurationMs())
                        .build())
                .collect(Collectors.toList());
    }

    private String simplifyContentType(String contentType) {
        if (contentType == null) {
            return "Unknown";
        }
        
        if (contentType.startsWith("image/")) {
            return "Images";
        } else if (contentType.startsWith("video/")) {
            return "Videos";
        } else if (contentType.startsWith("audio/")) {
            return "Audio";
        } else if (contentType.startsWith("text/") || contentType.contains("document") || 
                   contentType.contains("pdf") || contentType.contains("msword") ||
                   contentType.contains("excel") || contentType.contains("powerpoint")) {
            return "Documents";
        } else if (contentType.contains("zip") || contentType.contains("rar") || 
                   contentType.contains("7z") || contentType.contains("tar") ||
                   contentType.contains("gzip")) {
            return "Archives";
        } else if (contentType.contains("json") || contentType.contains("xml") ||
                   contentType.contains("javascript") || contentType.contains("html") ||
                   contentType.contains("css")) {
            return "Code";
        } else {
            return "Other";
        }
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
