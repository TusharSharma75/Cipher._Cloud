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
public class FileVersionDto {
    
    private Long id;
    private Integer versionNumber;
    private Long fileSize;
    private String formattedFileSize;
    private String sha256Hash;
    private IntegrityStatus integrityStatus;
    private LocalDateTime uploadTimestamp;
    private boolean isLatest;
}
