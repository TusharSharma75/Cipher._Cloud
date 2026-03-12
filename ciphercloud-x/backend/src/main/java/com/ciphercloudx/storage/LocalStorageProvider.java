package com.ciphercloudx.storage;

import com.ciphercloudx.enums.StorageProviderType;
import com.ciphercloudx.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Service
@Slf4j
public class LocalStorageProvider implements StorageProvider {

    @Value("${storage.local.path:./uploads}")
    private String storagePath;

    private Path basePath;

    @PostConstruct
    public void init() {
        try {
            basePath = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("Created local storage directory: {}", basePath);
            }
            log.info("Local storage provider initialized at: {}", basePath);
        } catch (IOException e) {
            throw new StorageException("Failed to initialize local storage", e);
        }
    }

    @Override
    public boolean upload(String key, InputStream inputStream, String contentType, long contentLength) {
        try {
            Path filePath = resolvePath(key);
            
            // Create parent directories if they don't exist
            Files.createDirectories(filePath.getParent());
            
            // Write file
            try (OutputStream outputStream = Files.newOutputStream(filePath, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                outputStream.flush();
                
                log.debug("Uploaded file to local storage: {} ({} bytes)", key, totalBytes);
            }
            
            return true;
        } catch (IOException e) {
            log.error("Failed to upload file to local storage: {}", key, e);
            return false;
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            Path filePath = resolvePath(key);
            if (!Files.exists(filePath)) {
                log.warn("File not found in local storage: {}", key);
                return null;
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("Failed to download file from local storage: {}", key, e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            Path filePath = resolvePath(key);
            if (!Files.exists(filePath)) {
                log.warn("File not found for deletion in local storage: {}", key);
                return true;
            }
            Files.delete(filePath);
            log.debug("Deleted file from local storage: {}", key);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete file from local storage: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        Path filePath = resolvePath(key);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    @Override
    public long getSize(String key) {
        try {
            Path filePath = resolvePath(key);
            if (!Files.exists(filePath)) {
                return -1;
            }
            return Files.size(filePath);
        } catch (IOException e) {
            log.error("Failed to get file size from local storage: {}", key, e);
            return -1;
        }
    }

    @Override
    public String getUrl(String key) {
        // Local storage doesn't provide URLs
        return null;
    }

    @Override
    public String getProviderType() {
        return StorageProviderType.LOCAL.name();
    }

    @Override
    public boolean isHealthy() {
        return Files.exists(basePath) && Files.isWritable(basePath);
    }

    @Override
    public String getName() {
        return "Local Storage";
    }

    /**
     * Resolve a storage key to a filesystem path
     */
    private Path resolvePath(String key) {
        // Sanitize the key to prevent path traversal
        String sanitizedKey = key.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // Create a nested directory structure based on the key
        // This helps avoid having too many files in a single directory
        String subDir1 = sanitizedKey.length() > 2 ? sanitizedKey.substring(0, 2) : "00";
        String subDir2 = sanitizedKey.length() > 4 ? sanitizedKey.substring(2, 4) : "00";
        
        return basePath.resolve(subDir1).resolve(subDir2).resolve(sanitizedKey);
    }

    /**
     * Get the base storage path
     */
    public Path getBasePath() {
        return basePath;
    }

    /**
     * Clean up empty directories
     */
    public void cleanupEmptyDirectories() {
        try {
            Files.walk(basePath)
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(basePath))
                    .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete children first
                    .forEach(path -> {
                        try {
                            if (Files.list(path).count() == 0) {
                                Files.delete(path);
                                log.debug("Deleted empty directory: {}", path);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to delete empty directory: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to cleanup empty directories", e);
        }
    }
}
