package com.ciphercloudx.entity;

import com.ciphercloudx.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean otpEnabled = false;

    @Column(name = "otp_secret")
    private String otpSecret;

    @ElementCollection
    @CollectionTable(name = "user_backup_codes", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "backup_code")
    private List<String> backupCodes = new ArrayList<>();

    @Column(name = "storage_quota", nullable = false)
    @Builder.Default
    private Long storageQuota = 1073741824L;

    @Column(name = "used_storage", nullable = false)
    @Builder.Default
    private Long usedStorage = 0L;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private boolean accountLocked = false;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "two_factor_verified", nullable = false)
    @Builder.Default
    private boolean twoFactorVerified = false;

    // NEW FIELDS FOR PASSWORD RESET

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expiry")
    private LocalDateTime passwordResetExpiry;

    @Column(name = "password_reset_otp")
    private String passwordResetOtp;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileMetadata> files = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActivityLog> activities = new ArrayList<>();

    @Version
    private Long version;

    public boolean hasAvailableStorage(long fileSize) {
        return (this.usedStorage + fileSize) <= this.storageQuota;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public void lockAccount() {
        this.accountLocked = true;
        this.lockTime = LocalDateTime.now();
    }

    public void unlockAccount() {
        this.accountLocked = false;
        this.lockTime = null;
        this.failedAttempts = 0;
    }

    public void addUsedStorage(long size) {
        this.usedStorage += size;
    }

    public void removeUsedStorage(long size) {
        this.usedStorage = Math.max(0, this.usedStorage - size);
    }
}