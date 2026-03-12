package com.ciphercloudx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUserDto {
    
    private Long userId;
    private String username;
    private String email;
    private Long value;
    private String formattedValue;
    private Long rank;
}
