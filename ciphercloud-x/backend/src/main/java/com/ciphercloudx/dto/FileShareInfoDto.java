package com.ciphercloudx.dto;

import com.ciphercloudx.enums.SharePermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileShareInfoDto {
    
    private Long id;
    private Long fileId;
    private String fileName;
    private Long sharedById;
    private String sharedByUsername;
    private Long sharedWithId;
    private String sharedWithUsername;
    private String shareToken;
    private SharePermission permission;
    private boolean isPublic;
    private boolean passwordProtected;
    private LocalDateTime expiryDate;
    private Integer maxDownloads;
    private Integer downloadCount;
    private boolean isActive;
    private boolean isExpired;
    private boolean hasReachedDownloadLimit;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;
    private String shareUrl;
}
