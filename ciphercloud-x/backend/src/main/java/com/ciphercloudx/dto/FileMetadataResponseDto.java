package com.ciphercloudx.dto;

import com.ciphercloudx.enums.IntegrityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataResponseDto {
    
    private Long id;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long fileSize;
    private String formattedFileSize;
    private Integer versionNumber;
    private Long parentFileId;
    private boolean isLatest;
    private String folderPath;
    
    private String sha256Hash;
    private IntegrityStatus integrityStatus;
    private String encryptionVersion;
    
    private String primaryLocation;
    private String backupLocation;
    private String replicationStatus;
    
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private String description;
    
    private LocalDateTime uploadTimestamp;
    private LocalDateTime updatedAt;
    
    private Long ownerId;
    private String ownerUsername;
    
    private List<FileVersionDto> versions;
    private boolean hasVersions;
    
    private boolean shared;
    private List<FileShareInfoDto> shares;
}
