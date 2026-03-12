package com.ciphercloudx.repository;

import com.ciphercloudx.entity.ActivityLog;
import com.ciphercloudx.enums.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    Page<ActivityLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    @Query("SELECT a FROM ActivityLog a ORDER BY a.timestamp DESC")
    Page<ActivityLog> findAllOrderByTimestampDesc(Pageable pageable);
    
    @Query("SELECT a FROM ActivityLog a WHERE a.user.id = :userId AND a.action = :action ORDER BY a.timestamp DESC")
    List<ActivityLog> findByUserIdAndAction(@Param("userId") Long userId, @Param("action") ActionType action);
    
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.action = :action AND a.timestamp >= :since")
    Long countByActionSince(@Param("action") ActionType action, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.action = :action AND a.status = :status AND a.timestamp >= :since")
    Long countByActionAndStatusSince(@Param("action") ActionType action, @Param("status") String status, @Param("since") LocalDateTime since);
    
    @Query("SELECT a.action, COUNT(a) FROM ActivityLog a WHERE a.timestamp >= :since GROUP BY a.action")
    List<Object[]> countByActionGroupedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT DATE(a.timestamp), COUNT(a) FROM ActivityLog a WHERE a.action = :action AND a.timestamp >= :since GROUP BY DATE(a.timestamp)")
    List<Object[]> countDailyByActionSince(@Param("action") ActionType action, @Param("since") LocalDateTime since);
    
    @Query("SELECT DATE(a.timestamp), COUNT(a), SUM(a.requestDurationMs) FROM ActivityLog a WHERE a.action = :action AND a.timestamp >= :since GROUP BY DATE(a.timestamp)")
    List<Object[]> countDailyWithSizeByActionSince(@Param("action") ActionType action, @Param("since") LocalDateTime since);
    
    @Query("SELECT a FROM ActivityLog a WHERE a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<ActivityLog> findRecentActivity(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.user.id = :userId AND a.action = :action")
    Long countByUserIdAndAction(@Param("userId") Long userId, @Param("action") ActionType action);
    
    @Query("SELECT a FROM ActivityLog a WHERE a.ipAddress = :ipAddress AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<ActivityLog> findByIpAddressSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.action = 'LOGIN' AND a.status = 'FAILURE' AND a.timestamp >= :since")
    Long countFailedLoginsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.action = 'UPLOAD' AND a.status = 'FAILURE' AND a.errorMessage LIKE '%quota%'")
    Long countQuotaViolations();
    
    @Query("SELECT a FROM ActivityLog a WHERE a.status = 'FAILURE' AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<ActivityLog> findRecentFailures(@Param("since") LocalDateTime since, Pageable pageable);
}
