package com.ciphercloudx.dto;

import com.ciphercloudx.enums.IntegrityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {
    
    private Long fileId;
    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private Long fileSize;
    private Integer versionNumber;
    private String folderPath;
    
    private String sha256Hash;
    private IntegrityStatus integrityStatus;
    private String encryptionVersion;
    
    private LocalDateTime uploadTimestamp;
    
    private Long storageQuota;
    private Long usedStorage;
    private Double storageUsagePercentage;
    private Boolean quotaExceeded;
    
    private String message;
}
