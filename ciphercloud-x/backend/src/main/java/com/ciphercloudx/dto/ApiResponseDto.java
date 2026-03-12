package com.ciphercloudx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;

    // ✅ SUCCESS with only data
    public static <T> ApiResponseDto<T> success(T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message("Request successful")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ✅ SUCCESS with data + custom message
    public static <T> ApiResponseDto<T> success(T data, String message) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ✅ ERROR with message only
    public static <T> ApiResponseDto<T> error(String message) {
        return ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ✅ ERROR with message + extra data (like validation errors map)
    public static <T> ApiResponseDto<T> error(String message, T data) {
        return ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Optional helper for GlobalExceptionHandler
    public ApiResponseDto<T> withTimestamp(LocalDateTime time) {
        this.timestamp = time;
        return this;
    }

    // Optional helper for setting path in exception handler
    public ApiResponseDto<T> setPath(String path) {
        this.path = path;
        return this;
    }
}