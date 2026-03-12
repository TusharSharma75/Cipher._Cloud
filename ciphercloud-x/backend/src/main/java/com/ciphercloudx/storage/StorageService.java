package com.ciphercloudx.storage;

import com.ciphercloudx.enums.StorageProviderType;
import com.ciphercloudx.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StorageService {

    @Value("${storage.primary-provider:BACKBLAZE_B2}")
    private String primaryProvider;

    @Value("${storage.backup-provider:}")
    private String backupProvider;

    @Value("${storage.enable-replication:false}")
    private boolean enableReplication;

    private final LocalStorageProvider localStorageProvider;
    private final BackblazeStorageProvider backblazeStorageProvider;
    private final SupabaseStorageProvider supabaseStorageProvider;

    @Autowired
    public StorageService(LocalStorageProvider localStorageProvider,
                          BackblazeStorageProvider backblazeStorageProvider,
                          SupabaseStorageProvider supabaseStorageProvider) {
        this.localStorageProvider = localStorageProvider;
        this.backblazeStorageProvider = backblazeStorageProvider;
        this.supabaseStorageProvider = supabaseStorageProvider;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // uploadToProvider — uploads to the specific provider chosen by the user
    // ─────────────────────────────────────────────────────────────────────────
    public StorageResult uploadToProvider(String key, InputStream inputStream,
                                          String contentType, long contentLength,
                                          String providerTypeName) {
        StorageProvider chosen = getProviderByType(providerTypeName);

        if (!chosen.isHealthy()) {
            log.warn("Chosen provider {} is not healthy, falling back to primary", providerTypeName);
            chosen = getPrimaryProvider();
        }

        boolean success = chosen.upload(key, inputStream, contentType, contentLength);

        if (!success) {
            throw new StorageException("Failed to upload file to " + providerTypeName);
        }

        log.info("File '{}' uploaded to {}", key, chosen.getName());

        return StorageResult.builder()
                .primaryLocation(chosen.getProviderType())
                .backupLocation(null)
                .replicationStatus("DISABLED")
                .success(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // upload — uses default primary provider (used by old code paths)
    // ─────────────────────────────────────────────────────────────────────────
    public StorageResult upload(String key, InputStream inputStream,
                                String contentType, long contentLength) {
        StorageProvider primary = getPrimaryProvider();

        if (!primary.isHealthy()) {
            log.warn("Primary provider {} not healthy, trying fallback", primary.getName());
            primary = getFallbackProvider();
        }

        boolean primarySuccess = primary.upload(key, inputStream, contentType, contentLength);
        if (!primarySuccess) {
            throw new StorageException("Failed to upload file to primary storage");
        }

        String primaryLocation = primary.getProviderType();
        String backupLocation = null;
        String replicationStatus;

        if (enableReplication && !backupProvider.isEmpty()) {
            StorageProvider backup = getBackupProvider();
            if (backup != null && backup.isHealthy()) {
                InputStream backupStream = primary.download(key);
                if (backupStream != null) {
                    boolean backupSuccess = backup.upload(key, backupStream, contentType, contentLength);
                    if (backupSuccess) {
                        backupLocation = backup.getProviderType();
                        replicationStatus = "COMPLETED";
                        log.info("Replicated {} → {}", primaryLocation, backupLocation);
                    } else {
                        replicationStatus = "FAILED";
                    }
                } else {
                    replicationStatus = "FAILED";
                }
            } else {
                replicationStatus = "FAILED";
            }
        } else {
            replicationStatus = "DISABLED";
        }

        return StorageResult.builder()
                .primaryLocation(primaryLocation)
                .backupLocation(backupLocation)
                .replicationStatus(replicationStatus)
                .success(true)
                .build();
    }

    public InputStream download(String key, String primaryLocation, String backupLocation) {
        StorageProvider primary = getProviderByType(primaryLocation);
        if (primary != null && primary.isHealthy()) {
            InputStream stream = primary.download(key);
            if (stream != null) return stream;
        }

        if (backupLocation != null && !backupLocation.isEmpty()) {
            StorageProvider backup = getProviderByType(backupLocation);
            if (backup != null && backup.isHealthy()) {
                InputStream stream = backup.download(key);
                if (stream != null) {
                    log.info("Downloaded '{}' from backup provider: {}", key, backupLocation);
                    return stream;
                }
            }
        }

        for (StorageProvider provider : getAllProviders()) {
            if (provider.isHealthy()) {
                InputStream stream = provider.download(key);
                if (stream != null) return stream;
            }
        }

        return null;
    }

    public boolean delete(String key, String primaryLocation, String backupLocation) {
        boolean deleted = false;

        StorageProvider primary = getProviderByType(primaryLocation);
        if (primary != null) deleted = primary.delete(key) || deleted;

        if (backupLocation != null && !backupLocation.isEmpty()) {
            StorageProvider backup = getProviderByType(backupLocation);
            if (backup != null) deleted = backup.delete(key) || deleted;
        }

        return deleted;
    }

    public boolean exists(String key, String primaryLocation, String backupLocation) {
        StorageProvider primary = getProviderByType(primaryLocation);
        if (primary != null && primary.exists(key)) return true;

        if (backupLocation != null && !backupLocation.isEmpty()) {
            StorageProvider backup = getProviderByType(backupLocation);
            if (backup != null && backup.exists(key)) return true;
        }

        return false;
    }

    public String getUrl(String key, String location) {
        StorageProvider provider = getProviderByType(location);
        return provider != null ? provider.getUrl(key) : null;
    }

    public List<StorageHealthStatus> getHealthStatus() {
        List<StorageHealthStatus> statuses = new ArrayList<>();
        for (StorageProvider provider : getAllProviders()) {
            statuses.add(StorageHealthStatus.builder()
                    .name(provider.getName())
                    .type(provider.getProviderType())
                    .healthy(provider.isHealthy())
                    .build());
        }
        return statuses;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private StorageProvider getPrimaryProvider() {
        return getProviderByType(primaryProvider);
    }

    private StorageProvider getBackupProvider() {
        if (backupProvider.isEmpty()) return null;
        return getProviderByType(backupProvider);
    }

    private StorageProvider getFallbackProvider() {
        for (StorageProvider provider : getAllProviders()) {
            if (!provider.getProviderType().equals(primaryProvider) && provider.isHealthy()) {
                return provider;
            }
        }
        return localStorageProvider;
    }

    private StorageProvider getProviderByType(String type) {
        if (type == null) return localStorageProvider;
        try {
            StorageProviderType providerType = StorageProviderType.valueOf(type);
            return switch (providerType) {
                case LOCAL -> localStorageProvider;
                case BACKBLAZE_B2 -> backblazeStorageProvider;
                case SUPABASE -> supabaseStorageProvider;
            };
        } catch (IllegalArgumentException e) {
            log.warn("Unknown provider type: {}, falling back to LOCAL", type);
            return localStorageProvider;
        }
    }

    private List<StorageProvider> getAllProviders() {
        List<StorageProvider> providers = new ArrayList<>();
        providers.add(localStorageProvider);
        if (backblazeStorageProvider.isInitialized()) providers.add(backblazeStorageProvider);
        if (supabaseStorageProvider.isInitialized()) providers.add(supabaseStorageProvider);
        return providers;
    }

    @lombok.Builder
    @lombok.Data
    public static class StorageResult {
        private String primaryLocation;
        private String backupLocation;
        private String replicationStatus;
        private boolean success;
    }

    @lombok.Builder
    @lombok.Data
    public static class StorageHealthStatus {
        private String name;
        private String type;
        private boolean healthy;
    }
}