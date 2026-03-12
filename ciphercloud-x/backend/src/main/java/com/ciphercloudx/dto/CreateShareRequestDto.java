package com.ciphercloudx.dto;

import com.ciphercloudx.enums.SharePermission;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateShareRequestDto {
    
    @NotNull(message = "File ID is required")
    private Long fileId;
    
    private Long sharedWithUserId;
    
    private SharePermission permission;
    
    private Boolean isPublic;
    
    private String password;
    
    private LocalDateTime expiryDate;
    
    private Integer maxDownloads;
}
