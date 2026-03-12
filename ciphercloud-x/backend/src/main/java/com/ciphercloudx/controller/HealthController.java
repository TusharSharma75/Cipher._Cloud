package com.ciphercloudx.controller;

import com.ciphercloudx.dto.ApiResponseDto;
import com.ciphercloudx.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final StorageService storageService;

    @Autowired
    public HealthController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "CipherCloud X");
        health.put("version", "1.0.0");
        
        List<StorageService.StorageHealthStatus> storageHealth = storageService.getHealthStatus();
        health.put("storage", storageHealth);
        
        return ResponseEntity.ok(ApiResponseDto.success(health));
    }
}
