package com.ciphercloudx.controller;

import com.ciphercloudx.dto.*;
import com.ciphercloudx.security.UserPrincipal;
import com.ciphercloudx.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ==========================
    // Current Logged-in User
    // ==========================
    @GetMapping("/user/me")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UserResponseDto user =
                userService.getCurrentUser(userPrincipal.getId());

        return ResponseEntity.ok(ApiResponseDto.success(user));
    }

    @PutMapping("/user/me")
    public ResponseEntity<ApiResponseDto<UserResponseDto>> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UserResponseDto user =
                userService.updateUser(userPrincipal.getId(), request);

        return ResponseEntity.ok(
                ApiResponseDto.success(user, "Profile updated successfully"));
    }

    // ==========================
    // 🔥 ADMIN: Get All Users (FIX FOR DASHBOARD)
    // ==========================
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Page<UserResponseDto>>> getAllUsers(
            Pageable pageable) {

        Page<UserResponseDto> users =
                userService.getAllUsers(pageable);

        return ResponseEntity.ok(ApiResponseDto.success(users));
    }

    // ==========================
    // ADMIN: Delete User
    // ==========================
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<String>> deleteUser(
            @PathVariable Long userId) {

        userService.deleteUser(userId);

        return ResponseEntity.ok(
                ApiResponseDto.success("User deleted successfully"));
    }
}