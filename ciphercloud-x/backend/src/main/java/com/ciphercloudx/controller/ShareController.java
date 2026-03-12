package com.ciphercloudx.controller;

import com.ciphercloudx.dto.*;
import com.ciphercloudx.security.UserPrincipal;
import com.ciphercloudx.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private final FileService fileService;

    @Autowired
    public ShareController(FileService fileService) {
        this.fileService = fileService;
    }

    // =========================
    // PUBLIC DOWNLOAD (no auth)
    // =========================

    @GetMapping("/public/{shareToken}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadSharedFile(
            @PathVariable String shareToken,
            @RequestParam(value = "password", required = false) String password,
            HttpServletRequest request) {

        FileDownloadResponseDto response = fileService.downloadSharedFile(shareToken, password, request);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + response.getOriginalFilename() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(response.getFileSize()))
                .header("X-Integrity-Verified", String.valueOf(response.isIntegrityVerified()))
                .body(new InputStreamResource(response.getInputStream()));
    }

    // =========================
    // CREATE SHARE LINK
    // POST /api/share
    // =========================

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<FileShareInfoDto>> createShare(
            @Valid @RequestBody CreateShareRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        FileShareInfoDto share = fileService.createShare(request, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(share, "Share link created successfully"));
    }

    // =========================
    // GET ALL SHARES FOR A FILE
    // GET /api/share/file/{fileId}
    // =========================

    @GetMapping("/file/{fileId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<List<FileShareInfoDto>>> getFileShares(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<FileShareInfoDto> shares = fileService.getFileShares(fileId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(shares));
    }

    // =========================
    // REVOKE SHARE
    // DELETE /api/share/{shareId}
    // =========================

    @DeleteMapping("/{shareId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDto<String>> revokeShare(
            @PathVariable Long shareId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        fileService.revokeShare(shareId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponseDto.success(null, "Share revoked successfully"));
    }
}