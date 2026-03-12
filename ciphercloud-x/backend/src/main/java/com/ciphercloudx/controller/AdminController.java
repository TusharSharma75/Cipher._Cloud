package com.ciphercloudx.controller;

import com.ciphercloudx.analytics.AnalyticsService;
import com.ciphercloudx.dto.*;
import com.ciphercloudx.enums.Role;
import com.ciphercloudx.quota.QuotaService;
import com.ciphercloudx.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final QuotaService quotaService;
    private final AnalyticsService analyticsService;

    @Autowired
    public AdminController(UserService userService,
                           QuotaService quotaService,
                           AnalyticsService analyticsService) {
        this.userService = userService;
        this.quotaService = quotaService;
        this.analyticsService = analyticsService;
    }

    // 🔥 ADD THIS METHOD
    @GetMapping("/users")
    public ResponseEntity<ApiResponseDto<Page<UserResponseDto>>> getAllUsers(Pageable pageable) {
        Page<UserResponseDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponseDto.success(users));
    }

    @PutMapping("/quota")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> updateQuota(
            @Valid @RequestBody QuotaUpdateRequestDto request) {

        UserResponseDto user = quotaService.updateUserQuota(request);

        return ResponseEntity.ok(
                ApiResponseDto.success(user, "Quota updated successfully"));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponseDto<String>> changeUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {

        userService.changeUserRole(userId, Role.valueOf(role));

        return ResponseEntity.ok(
                ApiResponseDto.success("User role updated successfully"));
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<ApiResponseDto<AnalyticsDashboardDto>> getDashboardAnalytics() {
        return ResponseEntity.ok(
                ApiResponseDto.success(analyticsService.getDashboardAnalytics()));
    }
}