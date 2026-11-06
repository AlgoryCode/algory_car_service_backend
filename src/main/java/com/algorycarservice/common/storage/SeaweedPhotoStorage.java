package com.algorycarservice.common.storage;

import com.algorycarservice.common.exception.ApiException;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.exception.ResourceNotFoundException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SeaweedFS, the same way qr-service talks to it: Filer HTTP ({@code PUT /buckets/<bucket>/<key>}, which creates the
 * bucket directory on first write) or, when S3 credentials are configured, the S3 gateway with path-style access.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "seaweed", matchIfMissing = true)
public class SeaweedPhotoStorage implements PhotoStorage {

    private final StorageProperties properties;
    private final RestClient filer;
    private final S3Client s3;
    private final AtomicBoolean bucketReady = new AtomicBoolean(false);

    public SeaweedPhotoStorage(StorageProperties properties) {
        this.properties = properties;
        this.filer = properties.isS3Mode() ? null : RestClient.builder().baseUrl(trimTrailingSlash(properties.filerUrl())).build();
        this.s3 = properties.isS3Mode() ? s3Client(properties) : null;
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        try {
            if (s3 != null) {
                ensureBucket();
                s3.putObject(PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(contentType).build(),
                        RequestBody.fromBytes(bytes));
            } else {
                filer.put().uri(filerPath(key)).contentType(MediaType.parseMediaType(contentType)).body(bytes)
                        .retrieve().toBodilessEntity();
            }
        } catch (RestClientException | SdkException ex) {
            log.error("Photo upload failed, bucket={} key={}", properties.bucket(), key, ex);
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    @Override
    public byte[] get(String key) {
        try {
            if (s3 != null) {
                return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(properties.bucket()).key(key).build()).asByteArray();
            }
            byte[] bytes = filer.get().uri(filerPath(key)).retrieve().body(byte[].class);
            if (bytes == null) {
                throw new ResourceNotFoundException("photo");
            }
            return bytes;
        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("photo");
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("photo");
            }
            log.error("Photo download failed, bucket={} key={}", properties.bucket(), key, ex);
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        } catch (RestClientException | SdkException ex) {
            log.error("Photo download failed, bucket={} key={}", properties.bucket(), key, ex);
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    @Override
    public void delete(String key) {
        try {
            if (s3 != null) {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(key).build());
            } else {
                filer.delete().uri(filerPath(key)).retrieve().toBodilessEntity();
            }
        } catch (RestClientException | SdkException ex) {
            log.warn("Photo delete failed, bucket={} key={}", properties.bucket(), key, ex);
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    @PreDestroy
    void close() {
        if (s3 != null) {
            s3.close();
        }
    }

    /** The S3 gateway does not create buckets on write, so the first upload makes sure ours exists. */
    private void ensureBucket() {
        if (bucketReady.get()) {
            return;
        }
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
        } catch (S3Exception ex) {
            if (ex.statusCode() != 404) {
                throw ex;
            }
            s3.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
        }
        bucketReady.set(true);
    }

    private String filerPath(String key) {
        return "/buckets/" + properties.bucket() + "/" + key;
    }

    private static S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(trimTrailingSlash(properties.s3Endpoint())))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
