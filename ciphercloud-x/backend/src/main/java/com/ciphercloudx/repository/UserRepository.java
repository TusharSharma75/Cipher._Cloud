package com.ciphercloudx.repository;

import com.ciphercloudx.entity.User;
import com.ciphercloudx.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.accountLocked = true AND u.lockTime < :lockTimeThreshold")
    List<User> findLockedAccountsBefore(@Param("lockTimeThreshold") LocalDateTime lockTimeThreshold);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    Long countNewUsersSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLogin >= :since")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.accountLocked = true")
    Long countLockedAccounts();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.otpEnabled = true")
    Long countOtpEnabledUsers();
    
    List<User> findByRole(Role role);
    
    Page<User> findAll(Pageable pageable);
    
    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = 0, u.accountLocked = false, u.lockTime = null WHERE u.id = :userId")
    void unlockAccount(@Param("userId") Long userId);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);
    
    @Query("SELECT SUM(u.usedStorage) FROM User u")
    Long sumUsedStorage();
    
    @Query("SELECT SUM(u.storageQuota) FROM User u")
    Long sumStorageQuota();
    
    @Query(value = "SELECT u.*, COUNT(f.id) as file_count FROM users u LEFT JOIN file_metadata f ON u.id = f.owner_id GROUP BY u.id ORDER BY file_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopUsersByFileCount(@Param("limit") int limit);
}
