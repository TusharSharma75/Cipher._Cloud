package com.ciphercloudx.entity;

import com.ciphercloudx.enums.IntegrityStatus;
import com.ciphercloudx.enums.StorageProviderType;
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
@Table(name = "file_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    
    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    private String storedFilename;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "version_number", nullable = false)
    @Builder.Default
    private Integer versionNumber = 1;
    
    @Column(name = "parent_file_id")
    private Long parentFileId;
    
    @Column(name = "is_latest", nullable = false)
    @Builder.Default
    private boolean isLatest = true;
    
    @Column(name = "encrypted_aes_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedAesKey;
    
    @Column(name = "iv", nullable = false, length = 64)
    private String iv;
    
    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "integrity_status", nullable = false, length = 20)
    @Builder.Default
    private IntegrityStatus integrityStatus = IntegrityStatus.VERIFIED;
    
    @Column(name = "encryption_version", nullable = false, length = 10)
    @Builder.Default
    private String encryptionVersion = "v1";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_location", nullable = false, length = 20)
    private StorageProviderType primaryLocation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "backup_location", length = 20)
    private StorageProviderType backupLocation;
    
    @Column(name = "replication_status", length = 20)
    @Builder.Default
    private String replicationStatus = "PENDING";
    
    @Column(name = "folder_path", length = 500)
    @Builder.Default
    private String folderPath = "/";
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @CreationTimestamp
    @Column(name = "upload_timestamp", nullable = false, updatable = false)
    private LocalDateTime uploadTimestamp;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileShare> shares = new ArrayList<>();
    
    @Version
    private Long version;
    
    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
    
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
    
    public String getFullPath() {
        return this.folderPath + this.originalFilename;
    }
}
