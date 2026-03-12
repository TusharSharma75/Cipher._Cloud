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
public class BackblazeStorageProvider implements StorageProvider {

    @Value("${storage.backblaze.enabled:false}")
    private boolean enabled;

    @Value("${storage.backblaze.endpoint:}")
    private String endpoint;

    @Value("${storage.backblaze.region:}")
    private String region;

    @Value("${storage.backblaze.bucket-name:}")
    private String bucketName;

    @Value("${storage.backblaze.key-id:}")
    private String keyId;

    @Value("${storage.backblaze.application-key:}")
    private String applicationKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Backblaze B2 disabled");
            return;
        }

        try {
            AwsBasicCredentials credentials =
                    AwsBasicCredentials.create(keyId, applicationKey);

            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(true)
                    .build();

            log.info("Backblaze B2 initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize Backblaze B2", e);
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

            return true;

        } catch (Exception e) {
            log.error("Backblaze upload failed", e);
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
            return new ByteArrayInputStream(bytes);

        } catch (Exception e) {
            log.error("Backblaze download failed", e);
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
            return true;

        } catch (Exception e) {
            log.error("Backblaze delete failed", e);
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
        return StorageProviderType.BACKBLAZE_B2.name();
    }

    @Override
    public boolean isHealthy() {
        return enabled && s3Client != null;
    }

    @Override
    public String getName() {
        return "Backblaze B2 Storage";
    }

    public boolean isInitialized() {
        return enabled && s3Client != null;
    }
}