package com.ciphercloudx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeStatDto {
    
    private String fileType;
    private Long count;
    private Long totalSize;
    private String formattedTotalSize;
    private Double percentage;
}
