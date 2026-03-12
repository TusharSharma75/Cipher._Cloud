package com.ciphercloudx.storage;

import com.ciphercloudx.enums.StorageProviderType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

@Service
@Slf4j
public class SupabaseStorageProvider implements StorageProvider {

    @Value("${storage.supabase.enabled:false}")
    private boolean enabled;

    @Value("${storage.supabase.endpoint:}")
    private String endpoint;

    @Value("${storage.supabase.region:}")
    private String region;

    @Value("${storage.supabase.bucket-name:}")
    private String bucketName;

    @Value("${storage.supabase.access-key-id:}")
    private String accessKeyId;

    @Value("${storage.supabase.secret-access-key:}")
    private String secretAccessKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Supabase Storage disabled");
            return;
        }

        try {
            AwsBasicCredentials credentials =
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey);

            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(true)
                    .build();

            log.info("Supabase Storage initialized successfully. Bucket: {}", bucketName);

        } catch (Exception e) {
            log.error("Failed to initialize Supabase Storage", e);
        }
    }

    @Override
    public boolean upload(String key, InputStream inputStream,
                          String contentType, long contentLength) {
        if (!enabled || s3Client == null) return false;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromInputStream(inputStream, contentLength));

            log.debug("Uploaded file to Supabase: {}", key);
            return true;

        } catch (Exception e) {
            log.error("Supabase upload failed for key: {}", key, e);
            return false;
        }
    }

    @Override
    public InputStream download(String key) {
        if (!enabled || s3Client == null) return null;

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response =
                    s3Client.getObject(request);

            byte[] bytes = response.readAllBytes();
            log.debug("Downloaded file from Supabase: {}", key);
            return new ByteArrayInputStream(bytes);

        } catch (Exception e) {
            log.error("Supabase download failed for key: {}", key, e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        if (!enabled || s3Client == null) return false;

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.debug("Deleted file from Supabase: {}", key);
            return true;

        } catch (Exception e) {
            log.error("Supabase delete failed for key: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean exists(String key) {
        if (!enabled || s3Client == null) return false;

        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getSize(String key) {
        if (!enabled || s3Client == null) return -1;

        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            return response.contentLength();

        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public String getUrl(String key) {
        if (!enabled) return null;
        return endpoint + "/" + bucketName + "/" + key;
    }

    @Override
    public String getProviderType() {
        return StorageProviderType.SUPABASE.name();
    }

    @Override
    public boolean isHealthy() {
        return enabled && s3Client != null;
    }

    @Override
    public String getName() {
        return "Supabase Storage";
    }

    public boolean isInitialized() {
        return enabled && s3Client != null;
    }
}