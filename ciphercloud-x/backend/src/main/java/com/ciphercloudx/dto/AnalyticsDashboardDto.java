package com.ciphercloudx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsDashboardDto {
    
    // User statistics
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;
    private Long lockedAccounts;
    private Long otpEnabledUsers;
    
    // Storage statistics
    private Long totalStorageUsed;
    private String formattedTotalStorageUsed;
    private Long totalStorageQuota;
    private String formattedTotalStorageQuota;
    private Double overallStorageUsagePercentage;
    private Long totalFiles;
    private Long totalDeletedFiles;
    
    // Activity statistics
    private Long uploadsToday;
    private Long downloadsToday;
    private Long loginsToday;
    private Long failedLoginsToday;
    private Long uploadsThisWeek;
    private Long downloadsThisWeek;
    private Long uploadsThisMonth;
    private Long downloadsThisMonth;
    
    // File type distribution
    private Map<String, Long> fileTypeDistribution;
    private List<FileTypeStatDto> fileTypeStats;
    
    // Upload trends
    private List<DailyActivityDto> uploadTrends;
    private List<DailyActivityDto> downloadTrends;
    private List<DailyActivityDto> loginTrends;
    
    // Top users
    private List<TopUserDto> topUsersByStorage;
    private List<TopUserDto> topUsersByUploads;
    
    // Recent activity
    private List<ActivityLogResponseDto> recentActivity;
    
    // Security metrics
    private Long integrityFailures;
    private Long quotaViolations;
    private Long suspiciousActivities;
}
