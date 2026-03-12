package com.ciphercloudx.controller;

import com.ciphercloudx.dto.*;
import com.ciphercloudx.security.UserPrincipal;
import com.ciphercloudx.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@PreAuthorize("isAuthenticated()")
public class FileController {

    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // =========================
    // UPLOAD — now accepts storageProvider param
    // =========================
    @PostMapping("/upload")
    public ResponseEntity<ApiResponseDto<FileUploadResponseDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderPath", defaultValue = "/") String folderPath,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "storageProvider", defaultValue = "BACKBLAZE_B2") String storageProvider,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) throws IOException {

        FileUploadResponseDto response = fileService.uploadFile(
                file, folderPath, description, storageProvider, userPrincipal.getId(), request);

        return ResponseEntity.ok(
                ApiResponseDto.success(response, "File uploaded successfully to " + storageProvider));
    }

    // =========================
    // DOWNLOAD
    // =========================
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {

        FileDownloadResponseDto file =
                fileService.downloadFile(fileId, userPrincipal.getId(), request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getFileSize())
                .body(new InputStreamResource(file.getInputStream()));
    }

    // =========================
    // GET FILES LIST
    // =========================
    @GetMapping
    public ResponseEntity<ApiResponseDto<Page<FileMetadataResponseDto>>> getUserFiles(
            @RequestParam(value = "folderPath", defaultValue = "/") String folderPath,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Page<FileMetadataResponseDto> files =
                fileService.getUserFiles(userPrincipal.getId(), folderPath, pageable);

        return ResponseEntity.ok(ApiResponseDto.success(files));
    }

    // =========================
    // GET SINGLE FILE METADATA
    // =========================
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponseDto<FileMetadataResponseDto>> getFileMetadata(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        FileMetadataResponseDto file =
                fileService.getFileMetadata(fileId, userPrincipal.getId());

        return ResponseEntity.ok(ApiResponseDto.success(file));
    }
}