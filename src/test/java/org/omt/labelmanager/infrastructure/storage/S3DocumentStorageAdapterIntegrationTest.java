package org.omt.labelmanager.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.application.RetrievedDocument;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Testcontainers
class S3DocumentStorageAdapterIntegrationTest {

    private static final String BUCKET = "test-costs";
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";

    @Container
    static MinIOContainer minIO = new MinIOContainer("minio/minio:latest")
            .withUserName(ACCESS_KEY)
            .withPassword(SECRET_KEY);

    private static S3Client s3Client;
    private S3DocumentStorageAdapter adapter;

    @BeforeAll
    static void setUpBucket() {
        s3Client = S3Client.builder()
                .endpointOverride(java.net.URI.create(minIO.getS3URL()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
                ))
                .forcePathStyle(true)
                .build();

        if (!bucketExists()) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    private static boolean bucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(BUCKET).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        var properties = new S3Properties(
                minIO.getS3URL(),
                BUCKET,
                "us-east-1",
                ACCESS_KEY,
                SECRET_KEY
        );
        adapter = new S3DocumentStorageAdapter(s3Client, properties);
    }

    @Test
    void storesDocumentAndReturnsKey() {
        String content = "invoice content";
        var inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String key = adapter.store("invoice.pdf", "application/pdf", inputStream);

        assertThat(key).startsWith("costs/").endsWith("/invoice.pdf");
    }

    @Test
    void storedDocumentCanBeRetrieved() throws Exception {
        String content = "invoice content";
        var inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String key = adapter.store("invoice.pdf", "application/pdf", inputStream);

        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()
        );
        String retrievedContent = new String(response.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(retrievedContent).isEqualTo(content);
    }

    @Test
    void setsCorrectContentType() {
        String content = "image data";
        var inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String key = adapter.store("photo.png", "image/png", inputStream);

        GetObjectResponse response = s3Client.getObject(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()
        ).response();

        assertThat(response.contentType()).isEqualTo("image/png");
    }

    @Test
    void generatesUniqueKeysForSameFilename() {
        String content = "invoice content";

        String key1 = adapter.store(
                "invoice.pdf",
                "application/pdf",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );
        String key2 = adapter.store(
                "invoice.pdf",
                "application/pdf",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void retrieveReturnsDocumentWithCorrectContent() throws Exception {
        String content = "invoice content";
        String key = adapter.store(
                "invoice.pdf",
                "application/pdf",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        RetrievedDocument document = adapter.retrieve(key);

        String retrievedContent = new String(document.content().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(retrievedContent).isEqualTo(content);
    }

    @Test
    void retrieveReturnsCorrectMetadata() throws Exception {
        String content = "invoice content";
        String key = adapter.store(
                "invoice.pdf",
                "application/pdf",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        RetrievedDocument document = adapter.retrieve(key);

        assertThat(document.filename()).isEqualTo("invoice.pdf");
        assertThat(document.contentType()).isEqualTo("application/pdf");
        assertThat(document.contentLength()).isEqualTo(content.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void deleteRemovesDocumentFromStorage() {
        String content = "invoice content";
        String key = adapter.store(
                "invoice.pdf",
                "application/pdf",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        adapter.delete(key);

        assertThat(objectExists(key)).isFalse();
    }

    private boolean objectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
