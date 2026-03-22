package org.omt.labelmanager.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
        String endpoint,
        String bucket,
        String region,
        String accessKey,
        String secretKey
) {
}
