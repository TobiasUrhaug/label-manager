package org.omt.labelmanager.infrastructure.storage;

import java.io.InputStream;
import java.util.UUID;
import org.omt.labelmanager.finance.application.DocumentStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3DocumentStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3DocumentStorageAdapter.class);

    private final S3Client s3Client;
    private final S3Properties properties;

    public S3DocumentStorageAdapter(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public String store(String filename, String contentType, InputStream content) {
        String key = generateKey(filename);
        log.info("Storing document '{}' with key '{}'", filename, key);

        try {
            byte[] bytes = content.readAllBytes();

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            log.debug("Document stored successfully");

            return key;
        } catch (Exception e) {
            log.error("Failed to store document '{}': {}", filename, e.getMessage());
            throw new DocumentStorageException("Failed to store document: " + filename, e);
        }
    }

    private String generateKey(String filename) {
        return "costs/" + UUID.randomUUID() + "/" + filename;
    }
}
