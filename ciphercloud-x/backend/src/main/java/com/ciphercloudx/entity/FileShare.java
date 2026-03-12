package com.ciphercloudx.entity;

import com.ciphercloudx.enums.SharePermission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_shares", indexes = {
    @Index(name = "idx_file_share", columnList = "file_id, share_token"),
    @Index(name = "idx_shared_with", columnList = "shared_with_user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShare {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id")
    private User sharedWith;
    
    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    private String shareToken;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SharePermission permission = SharePermission.READ;
    
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;
    
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
    
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
    
    @Column(name = "max_downloads")
    private Integer maxDownloads;
    
    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;
    
    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    public boolean hasReachedDownloadLimit() {
        if (maxDownloads == null) {
            return false;
        }
        return downloadCount >= maxDownloads;
    }
    
    public boolean isValid() {
        return isActive && !isExpired() && !hasReachedDownloadLimit();
    }
    
    public void incrementDownloadCount() {
        this.downloadCount++;
        this.lastAccessed = LocalDateTime.now();
    }
}
