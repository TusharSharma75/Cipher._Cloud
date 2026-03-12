package com.ciphercloudx.service;

import com.ciphercloudx.audit.AuditService;
import com.ciphercloudx.dto.*;
import org.springframework.security.access.AccessDeniedException;
import com.ciphercloudx.encryption.EncryptionService;
import com.ciphercloudx.entity.FileMetadata;
import com.ciphercloudx.entity.FileShare;
import com.ciphercloudx.entity.User;
import com.ciphercloudx.enums.ActionType;
import com.ciphercloudx.enums.IntegrityStatus;
import com.ciphercloudx.enums.SharePermission;
import com.ciphercloudx.enums.StorageProviderType;
import com.ciphercloudx.exception.*;
import com.ciphercloudx.repository.FileMetadataRepository;
import com.ciphercloudx.repository.FileShareRepository;
import com.ciphercloudx.repository.UserRepository;
import com.ciphercloudx.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileShareRepository fileShareRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final StorageService storageService;
    private final AuditService auditService;

    @Value("${storage.max-file-size:104857600}")
    private long maxFileSize;

    @Autowired
    public FileService(FileMetadataRepository fileMetadataRepository,
                       FileShareRepository fileShareRepository,
                       UserRepository userRepository,
                       EncryptionService encryptionService,
                       StorageService storageService,
                       AuditService auditService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileShareRepository = fileShareRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOAD — storageProvider overrides the default primary provider
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public FileUploadResponseDto uploadFile(MultipartFile file, String folderPath,
                                            String description, String storageProvider,
                                            Long userId, HttpServletRequest request) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }
        if (!user.hasAvailableStorage(file.getSize())) {
            auditService.logActivityFailure(user, ActionType.QUOTA_EXCEEDED,
                    "Storage quota exceeded", request);
            throw new QuotaExceededException("Storage quota exceeded.",
                    user.getUsedStorage(), user.getStorageQuota(), file.getSize());
        }

        // Validate provider type — fallback to BACKBLAZE_B2 if invalid
        StorageProviderType providerType;
        try {
            providerType = StorageProviderType.valueOf(storageProvider);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid storageProvider '{}', falling back to BACKBLAZE_B2", storageProvider);
            providerType = StorageProviderType.BACKBLAZE_B2;
        }

        String sanitizedFolderPath = sanitizeFolderPath(folderPath);
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storedFilename = generateStoredFilename(originalFilename);

        byte[] fileData = file.getBytes();
        String sha256Hash = encryptionService.calculateHash(fileData);

        EncryptionService.EncryptionResult encryptionResult = encryptionService.encryptFile(fileData);

        // Upload to the user-chosen cloud
        StorageService.StorageResult storageResult;
        try (InputStream encryptedStream = new ByteArrayInputStream(encryptionResult.getEncryptedData())) {
            storageResult = storageService.uploadToProvider(
                    storedFilename,
                    encryptedStream,
                    file.getContentType(),
                    encryptionResult.getEncryptedData().length,
                    providerType.name()          // ← pass chosen provider
            );
        }

        FileMetadata fileMetadata = FileMetadata.builder()
                .owner(user)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .versionNumber(1)
                .isLatest(true)
                .encryptedAesKey(encryptionResult.getEncryptedKey())
                .iv(encryptionResult.getIv())
                .sha256Hash(sha256Hash)
                .integrityStatus(IntegrityStatus.VERIFIED)
                .encryptionVersion("v1")
                .primaryLocation(StorageProviderType.valueOf(storageResult.getPrimaryLocation()))
                .backupLocation(storageResult.getBackupLocation() != null ?
                        StorageProviderType.valueOf(storageResult.getBackupLocation()) : null)
                .replicationStatus(storageResult.getReplicationStatus())
                .folderPath(sanitizedFolderPath)
                .description(description)
                .build();

        fileMetadataRepository.save(fileMetadata);

        user.addUsedStorage(file.getSize());
        userRepository.save(user);

        auditService.logActivity(user, ActionType.UPLOAD, fileMetadata.getId(),
                originalFilename + " [" + providerType.name() + "]", request);

        log.info("File '{}' uploaded to {} by user: {}",
                originalFilename, providerType.name(), user.getUsername());

        double storageUsagePercentage = user.getStorageQuota() > 0
                ? (double) user.getUsedStorage() / user.getStorageQuota() * 100
                : 0;

        return FileUploadResponseDto.builder()
                .fileId(fileMetadata.getId())
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .versionNumber(1)
                .folderPath(sanitizedFolderPath)
                .sha256Hash(sha256Hash)
                .integrityStatus(IntegrityStatus.VERIFIED)
                .encryptionVersion("v1")
                .uploadTimestamp(fileMetadata.getUploadTimestamp())
                .storageQuota(user.getStorageQuota())
                .usedStorage(user.getUsedStorage())
                .storageUsagePercentage(Math.round(storageUsagePercentage * 100.0) / 100.0)
                .quotaExceeded(!user.hasAvailableStorage(0))
                .message("File uploaded successfully to " + providerType.name())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DOWNLOAD
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public FileDownloadResponseDto downloadFile(Long fileId, Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileMetadata.getOwner().getId().equals(userId)) {
            boolean hasAccess = fileShareRepository.findActiveSharesByFileId(fileId).stream()
                    .anyMatch(share -> share.getSharedWith() != null &&
                            share.getSharedWith().getId().equals(userId) &&
                            share.isValid());
            if (!hasAccess) {
                throw new AccessDeniedException("You don't have permission to download this file");
            }
        }

        InputStream encryptedStream = storageService.download(fileMetadata.getStoredFilename(),
                fileMetadata.getPrimaryLocation().name(),
                fileMetadata.getBackupLocation() != null ? fileMetadata.getBackupLocation().name() : null);

        if (encryptedStream == null) {
            throw new StorageException("File not found in storage");
        }

        try {
            byte[] encryptedData = encryptedStream.readAllBytes();
            byte[] decryptedData = encryptionService.decryptFile(encryptedData,
                    fileMetadata.getEncryptedAesKey(), fileMetadata.getIv());

            String computedHash = encryptionService.calculateHash(decryptedData);
            boolean integrityVerified = computedHash.equals(fileMetadata.getSha256Hash());

            if (!integrityVerified) {
                fileMetadata.setIntegrityStatus(IntegrityStatus.FAILED);
                fileMetadataRepository.save(fileMetadata);
                auditService.logActivityFailure(user, ActionType.DOWNLOAD,
                        "Integrity check failed", request);
                throw new IntegrityCheckException("File integrity check failed",
                        fileMetadata.getSha256Hash(), computedHash);
            }

            auditService.logActivity(user, ActionType.DOWNLOAD, fileId,
                    fileMetadata.getOriginalFilename(), request);

            return FileDownloadResponseDto.builder()
                    .originalFilename(fileMetadata.getOriginalFilename())
                    .contentType(fileMetadata.getContentType())
                    .fileSize(fileMetadata.getFileSize())
                    .inputStream(new ByteArrayInputStream(decryptedData))
                    .integrityVerified(true)
                    .sha256Hash(fileMetadata.getSha256Hash())
                    .build();

        } catch (IOException e) {
            throw new StorageException("Failed to read file data", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void deleteFile(Long fileId, Long userId, boolean permanent, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileMetadata.getOwner().getId().equals(userId) && !user.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("You don't have permission to delete this file");
        }

        if (permanent) {
            storageService.delete(fileMetadata.getStoredFilename(),
                    fileMetadata.getPrimaryLocation().name(),
                    fileMetadata.getBackupLocation() != null ? fileMetadata.getBackupLocation().name() : null);

            List<FileMetadata> versions = fileMetadataRepository.findVersionsByParentFileId(fileId);
            for (FileMetadata version : versions) {
                storageService.delete(version.getStoredFilename(),
                        version.getPrimaryLocation().name(),
                        version.getBackupLocation() != null ? version.getBackupLocation().name() : null);
            }

            long totalSize = versions.stream().mapToLong(FileMetadata::getFileSize).sum();
            user.removeUsedStorage(totalSize);
            userRepository.save(user);
            fileMetadataRepository.delete(fileMetadata);
        } else {
            fileMetadata.markAsDeleted();
            fileMetadataRepository.save(fileMetadata);
        }

        auditService.logActivity(user, ActionType.DELETE, fileId,
                fileMetadata.getOriginalFilename(), request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET FILES
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<FileMetadataResponseDto> getUserFiles(Long userId, String folderPath, Pageable pageable) {
        String sanitizedPath = sanitizeFolderPath(folderPath);
        return fileMetadataRepository.findByOwnerIdAndFolderPath(userId, sanitizedPath).stream()
                .filter(f -> !f.isDeleted())
                .map(this::mapToFileMetadataResponse)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> new org.springframework.data.domain.PageImpl<>(list, pageable, list.size())
                ));
    }

    @Transactional(readOnly = true)
    public FileMetadataResponseDto getFileMetadata(Long fileId, Long userId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileMetadata.getOwner().getId().equals(userId)) {
            boolean hasAccess = fileShareRepository.findActiveSharesByFileId(fileId).stream()
                    .anyMatch(share -> share.getSharedWith() != null &&
                            share.getSharedWith().getId().equals(userId));
            if (!hasAccess) {
                throw new AccessDeniedException("You don't have permission to view this file");
            }
        }

        return mapToFileMetadataResponse(fileMetadata);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERSIONING
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public FileMetadataResponseDto createNewVersion(Long fileId, MultipartFile file,
                                                     Long userId, HttpServletRequest request) throws IOException {
        FileMetadata existingFile = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!existingFile.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to update this file");
        }

        fileMetadataRepository.clearLatestVersionFlag(fileId);

        // Keep same provider as original file for new versions
        String providerName = existingFile.getPrimaryLocation().name();
        FileUploadResponseDto uploadResult = uploadFile(file, existingFile.getFolderPath(),
                existingFile.getDescription(), providerName, userId, request);

        FileMetadata newVersion = fileMetadataRepository.findById(uploadResult.getFileId())
                .orElseThrow(() -> new FileNotFoundException("New version not found"));

        newVersion.setParentFileId(fileId);
        newVersion.setVersionNumber(getNextVersionNumber(fileId));
        newVersion.setLatest(true);
        fileMetadataRepository.save(newVersion);

        return mapToFileMetadataResponse(newVersion);
    }

    @Transactional
    public FileMetadataResponseDto rollbackToVersion(Long fileId, Integer versionNumber, Long userId) {
        FileMetadata currentFile = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!currentFile.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to rollback this file");
        }

        List<FileMetadata> versions = fileMetadataRepository.findVersionsByParentFileId(fileId);
        FileMetadata targetVersion = versions.stream()
                .filter(v -> v.getVersionNumber().equals(versionNumber))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("Version not found"));

        fileMetadataRepository.clearLatestVersionFlag(fileId);
        targetVersion.setLatest(true);
        fileMetadataRepository.save(targetVersion);

        auditService.logActivity(userRepository.findById(userId).get(),
                ActionType.VERSION_RESTORE, fileId, currentFile.getOriginalFilename(), null);

        return mapToFileMetadataResponse(targetVersion);
    }

    @Transactional(readOnly = true)
    public List<FileVersionDto> getFileVersions(Long fileId, Long userId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileMetadata.getOwner().getId().equals(userId)) {
            boolean hasAccess = fileShareRepository.findActiveSharesByFileId(fileId).stream()
                    .anyMatch(share -> share.getSharedWith() != null &&
                            share.getSharedWith().getId().equals(userId));
            if (!hasAccess) {
                throw new AccessDeniedException("You don't have permission to view versions");
            }
        }

        return fileMetadataRepository.findVersionsByParentFileId(fileId).stream()
                .map(this::mapToFileVersionDto)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARES
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public FileShareInfoDto createShare(CreateShareRequestDto request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        FileMetadata fileMetadata = fileMetadataRepository.findById(request.getFileId())
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileMetadata.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to share this file");
        }

        String shareToken = encryptionService.generateSecureToken(32);

        FileShare share = FileShare.builder()
                .file(fileMetadata)
                .sharedBy(user)
                .sharedWith(request.getSharedWithUserId() != null ?
                        userRepository.findById(request.getSharedWithUserId()).orElse(null) : null)
                .shareToken(shareToken)
                .permission(request.getPermission() != null ? request.getPermission() : SharePermission.READ)
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .passwordHash(request.getPassword() != null ?
                        passwordEncoder().encode(request.getPassword()) : null)
                .expiryDate(request.getExpiryDate())
                .maxDownloads(request.getMaxDownloads())
                .isActive(true)
                .build();

        fileShareRepository.save(share);
        auditService.logActivity(user, ActionType.SHARE, request.getFileId(),
                fileMetadata.getOriginalFilename(), null);

        return mapToFileShareInfoDto(share);
    }

    @Transactional
    public void revokeShare(Long shareId, Long userId) {
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new FileNotFoundException("Share not found"));

        if (!share.getSharedBy().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to revoke this share");
        }

        share.setActive(false);
        fileShareRepository.save(share);

        auditService.logActivity(share.getSharedBy(), ActionType.UNSHARE,
                share.getFile().getId(), share.getFile().getOriginalFilename(), null);
    }

    @Transactional(readOnly = true)
    public List<FileShareInfoDto> getFileShares(Long fileId, Long userId) {
        FileMetadata fileMetadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileMetadata.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have permission to view this file's shares");
        }

        return fileShareRepository.findActiveSharesByFileId(fileId).stream()
                .map(this::mapToFileShareInfoDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FileDownloadResponseDto downloadSharedFile(String shareToken, String password,
                                                       HttpServletRequest request) {
        FileShare share = fileShareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new FileNotFoundException("Share not found"));

        if (!share.isValid()) {
            throw new AccessDeniedException("This share link is no longer valid");
        }

        if (share.getPasswordHash() != null) {
            if (password == null || !passwordEncoder().matches(password, share.getPasswordHash())) {
                throw new AccessDeniedException("Invalid password");
            }
        }

        FileMetadata fileMetadata = share.getFile();
        InputStream encryptedStream = storageService.download(fileMetadata.getStoredFilename(),
                fileMetadata.getPrimaryLocation().name(),
                fileMetadata.getBackupLocation() != null ? fileMetadata.getBackupLocation().name() : null);

        if (encryptedStream == null) {
            throw new StorageException("File not found in storage");
        }

        try {
            byte[] encryptedData = encryptedStream.readAllBytes();
            byte[] decryptedData = encryptionService.decryptFile(encryptedData,
                    fileMetadata.getEncryptedAesKey(), fileMetadata.getIv());

            share.incrementDownloadCount();
            fileShareRepository.save(share);

            return FileDownloadResponseDto.builder()
                    .originalFilename(fileMetadata.getOriginalFilename())
                    .contentType(fileMetadata.getContentType())
                    .fileSize(fileMetadata.getFileSize())
                    .inputStream(new ByteArrayInputStream(decryptedData))
                    .integrityVerified(true)
                    .sha256Hash(fileMetadata.getSha256Hash())
                    .build();

        } catch (IOException e) {
            throw new StorageException("Failed to read file data", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        String sanitized = Paths.get(filename).getFileName().toString();
        sanitized = sanitized.replaceAll("[<>:\\\"|?*]", "_");
        if (sanitized.length() > 255) sanitized = sanitized.substring(0, 255);
        return sanitized;
    }

    private String sanitizeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) return "/";
        Path path = Paths.get(folderPath).normalize();
        String normalized = path.toString().replace("\\", "/");
        if (!normalized.startsWith("/")) normalized = "/" + normalized;
        if (!normalized.endsWith("/")) normalized = normalized + "/";
        if (normalized.contains("..") || normalized.contains("~")) {
            throw new IllegalArgumentException("Invalid folder path");
        }
        return normalized;
    }

    private String generateStoredFilename(String originalFilename) {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) extension = originalFilename.substring(lastDot);
        return UUID.randomUUID().toString() + extension;
    }

    private int getNextVersionNumber(Long parentFileId) {
        return fileMetadataRepository.findVersionsByParentFileId(parentFileId).stream()
                .mapToInt(FileMetadata::getVersionNumber)
                .max().orElse(0) + 1;
    }

    private FileMetadataResponseDto mapToFileMetadataResponse(FileMetadata file) {
        List<FileVersionDto> versions = fileMetadataRepository
                .findVersionsByParentFileId(file.getId()).stream()
                .map(this::mapToFileVersionDto)
                .collect(Collectors.toList());

        List<FileShareInfoDto> shares = fileShareRepository
                .findActiveSharesByFileId(file.getId()).stream()
                .map(this::mapToFileShareInfoDto)
                .collect(Collectors.toList());

        return FileMetadataResponseDto.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .storedFilename(file.getStoredFilename())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .formattedFileSize(formatFileSize(file.getFileSize()))
                .versionNumber(file.getVersionNumber())
                .parentFileId(file.getParentFileId())
                .isLatest(file.isLatest())
                .folderPath(file.getFolderPath())
                .sha256Hash(file.getSha256Hash())
                .integrityStatus(file.getIntegrityStatus())
                .encryptionVersion(file.getEncryptionVersion())
                .primaryLocation(file.getPrimaryLocation().name())
                .backupLocation(file.getBackupLocation() != null ? file.getBackupLocation().name() : null)
                .replicationStatus(file.getReplicationStatus())
                .isDeleted(file.isDeleted())
                .deletedAt(file.getDeletedAt())
                .description(file.getDescription())
                .uploadTimestamp(file.getUploadTimestamp())
                .updatedAt(file.getUpdatedAt())
                .ownerId(file.getOwner().getId())
                .ownerUsername(file.getOwner().getUsername())
                .versions(versions)
                .hasVersions(!versions.isEmpty())
                .shared(!shares.isEmpty())
                .shares(shares)
                .build();
    }

    private FileVersionDto mapToFileVersionDto(FileMetadata file) {
        return FileVersionDto.builder()
                .id(file.getId())
                .versionNumber(file.getVersionNumber())
                .fileSize(file.getFileSize())
                .formattedFileSize(formatFileSize(file.getFileSize()))
                .sha256Hash(file.getSha256Hash())
                .integrityStatus(file.getIntegrityStatus())
                .uploadTimestamp(file.getUploadTimestamp())
                .isLatest(file.isLatest())
                .build();
    }

    private FileShareInfoDto mapToFileShareInfoDto(FileShare share) {
        return FileShareInfoDto.builder()
                .id(share.getId())
                .fileId(share.getFile().getId())
                .fileName(share.getFile().getOriginalFilename())
                .sharedById(share.getSharedBy().getId())
                .sharedByUsername(share.getSharedBy().getUsername())
                .sharedWithId(share.getSharedWith() != null ? share.getSharedWith().getId() : null)
                .sharedWithUsername(share.getSharedWith() != null ? share.getSharedWith().getUsername() : null)
                .shareToken(share.getShareToken())
                .permission(share.getPermission())
                .isPublic(share.isPublic())
                .passwordProtected(share.getPasswordHash() != null)
                .expiryDate(share.getExpiryDate())
                .maxDownloads(share.getMaxDownloads())
                .downloadCount(share.getDownloadCount())
                .isActive(share.isActive())
                .isExpired(share.isExpired())
                .hasReachedDownloadLimit(share.hasReachedDownloadLimit())
                .createdAt(share.getCreatedAt())
                .lastAccessed(share.getLastAccessed())
                .shareUrl("/api/share/public/" + share.getShareToken())
                .build();
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        else if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        else return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}