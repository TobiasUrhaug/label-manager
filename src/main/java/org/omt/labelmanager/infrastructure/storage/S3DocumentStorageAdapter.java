package org.omt.labelmanager.infrastructure.storage;

import java.io.InputStream;
import java.util.UUID;
import org.omt.labelmanager.finance.cost.ports.DocumentStoragePort;
import org.omt.labelmanager.finance.cost.RetrievedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
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

    @Override
    public RetrievedDocument retrieve(String storageKey) {
        log.info("Retrieving document with key '{}'", storageKey);

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            GetObjectResponse metadata = response.response();

            String filename = extractFilename(storageKey);
            log.debug("Document retrieved: filename='{}', contentType='{}'",
                    filename, metadata.contentType());

            return new RetrievedDocument(
                    response,
                    metadata.contentType(),
                    filename,
                    metadata.contentLength()
            );
        } catch (Exception e) {
            log.error("Failed to retrieve document '{}': {}", storageKey, e.getMessage());
            throw new DocumentStorageException("Failed to retrieve document: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        log.info("Deleting document with key '{}'", storageKey);

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();

            s3Client.deleteObject(request);
            log.debug("Document deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete document '{}': {}", storageKey, e.getMessage());
            throw new DocumentStorageException("Failed to delete document: " + storageKey, e);
        }
    }

    private String generateKey(String filename) {
        return "costs/" + UUID.randomUUID() + "/" + filename;
    }

    private String extractFilename(String storageKey) {
        int lastSlash = storageKey.lastIndexOf('/');
        return lastSlash >= 0 ? storageKey.substring(lastSlash + 1) : storageKey;
    }
}
