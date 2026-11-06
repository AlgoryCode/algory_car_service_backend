package com.algorycarservice.common.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Where uploaded photos go: the SeaweedFS that qr-service also uses, in a bucket of our own. Locally the Filer HTTP
 * API is enough; with S3 credentials the S3 gateway is used instead, as in production.
 *
 * @param type          {@code memory} keeps files in the JVM, for tests only
 * @param filerUrl      SeaweedFS Filer, e.g. {@code http://localhost:8888}; objects live under {@code /buckets/<bucket>/}
 * @param s3Endpoint    S3 gateway; used together with the keys when all three are set
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @NotNull Type type,
        String filerUrl,
        String s3Endpoint,
        String accessKey,
        String secretKey,
        @NotBlank String bucket,
        @Positive long maxFileSizeBytes
) {

    public enum Type { SEAWEED, MEMORY }

    public boolean isS3Mode() {
        return StringUtils.hasText(s3Endpoint) && StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
    }
}
