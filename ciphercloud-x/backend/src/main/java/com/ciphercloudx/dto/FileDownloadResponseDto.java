package com.ciphercloudx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadResponseDto {
    
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private InputStream inputStream;
    private boolean integrityVerified;
    private String sha256Hash;
}
