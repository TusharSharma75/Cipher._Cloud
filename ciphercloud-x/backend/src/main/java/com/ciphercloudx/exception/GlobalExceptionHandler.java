package com.ciphercloudx.exception;

import com.ciphercloudx.dto.ApiResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Object>> handleException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Error: ", ex);

        ApiResponseDto<Object> response =
                ApiResponseDto.error("An unexpected error occurred");

        response.setTimestamp(LocalDateTime.now());
        response.setPath(request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}