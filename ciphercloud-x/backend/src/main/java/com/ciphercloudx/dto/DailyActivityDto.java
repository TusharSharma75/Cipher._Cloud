package com.ciphercloudx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivityDto {
    
    private LocalDate date;
    private Long count;
    private Long totalSize;
    private String formattedTotalSize;
}
