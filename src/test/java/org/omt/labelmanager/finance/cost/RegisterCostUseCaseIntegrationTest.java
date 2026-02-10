package org.omt.labelmanager.finance.cost;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.finance.cost.domain.CostOwner;
import org.omt.labelmanager.finance.cost.domain.CostOwnerType;
import org.omt.labelmanager.finance.cost.domain.CostType;
import org.omt.labelmanager.finance.cost.domain.VatAmount;
import org.omt.labelmanager.finance.cost.features.RegisterCostUseCase;
import org.omt.labelmanager.finance.cost.persistence.CostRepository;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RegisterCostUseCaseIntegrationTest {

    private static final String BUCKET = "costs";
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";

    @Autowired
    RegisterCostUseCase registerCostUseCase;

    @Autowired
    CostRepository costRepository;

    @Autowired
    LabelTestHelper labelTestHelper;

    @Autowired
    ReleaseTestHelper releaseTestHelper;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static MinIOContainer minIO = new MinIOContainer("minio/minio:latest")
            .withUserName(MINIO_ACCESS_KEY)
            .withPassword(MINIO_SECRET_KEY);

    @BeforeAll
    static void setUpBucket() {
        S3Client s3Client = S3Client.builder()
                .endpointOverride(java.net.URI.create(minIO.getS3URL()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
                ))
                .forcePathStyle(true)
                .build();

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(BUCKET).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("storage.s3.endpoint", minIO::getS3URL);
        registry.add("storage.s3.bucket", () -> BUCKET);
        registry.add("storage.s3.region", () -> "us-east-1");
        registry.add("storage.s3.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("storage.s3.secret-key", () -> MINIO_SECRET_KEY);
    }

    @Test
    void registersCostForRelease() {
        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        registerCostUseCase.registerCost(
                Money.of(new BigDecimal("100.00")),
                new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("125.00")),
                CostType.MASTERING,
                LocalDate.of(2024, 6, 15),
                "Mastering for album",
                CostOwner.release(releaseId),
                "INV-2024-001"
        );

        var costs = costRepository.findByOwnerOwnerTypeAndOwnerOwnerId(
                CostOwnerType.RELEASE, releaseId);
        assertThat(costs).hasSize(1);
        assertThat(costs.getFirst().getNetAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(costs.getFirst().getDescription()).isEqualTo("Mastering for album");
    }

    @Test
    void registersCostForLabel() {
        var label = labelTestHelper.createLabel("Label With Cost");

        registerCostUseCase.registerCost(
                Money.of(new BigDecimal("50.00")),
                new VatAmount(Money.of(new BigDecimal("12.50")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("62.50")),
                CostType.HOSTING,
                LocalDate.of(2024, 7, 1),
                "Website hosting",
                CostOwner.label(label.id()),
                null
        );

        var costs = costRepository.findByOwnerOwnerTypeAndOwnerOwnerId(
                CostOwnerType.LABEL, label.id());
        assertThat(costs).hasSize(1);
        assertThat(costs.getFirst().getCostType()).isEqualTo(CostType.HOSTING);
    }

    @Test
    void throwsWhenReleaseNotFound() {
        assertThatThrownBy(() -> registerCostUseCase.registerCost(
                Money.of(new BigDecimal("100.00")),
                new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("125.00")),
                CostType.MASTERING,
                LocalDate.of(2024, 6, 15),
                "Mastering",
                CostOwner.release(99999L),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenLabelNotFound() {
        assertThatThrownBy(() -> registerCostUseCase.registerCost(
                Money.of(new BigDecimal("100.00")),
                new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("125.00")),
                CostType.HOSTING,
                LocalDate.of(2024, 6, 15),
                "Hosting",
                CostOwner.label(99999L),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registersCostWithDocumentUpload() {
        var label = labelTestHelper.createLabel("Label With Document");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Release With Invoice", label.id());

        String documentContent = "Invoice PDF content";
        var document = new DocumentUpload(
                "invoice.pdf",
                "application/pdf",
                new ByteArrayInputStream(documentContent.getBytes(StandardCharsets.UTF_8))
        );

        registerCostUseCase.registerCost(
                Money.of(new BigDecimal("200.00")),
                new VatAmount(Money.of(new BigDecimal("50.00")), new BigDecimal("0.25")),
                Money.of(new BigDecimal("250.00")),
                CostType.MASTERING,
                LocalDate.of(2024, 8, 20),
                "Mastering with invoice",
                CostOwner.release(releaseId),
                "INV-2024-001",
                document
        );

        var costs = costRepository.findByOwnerOwnerTypeAndOwnerOwnerId(
                CostOwnerType.RELEASE, releaseId);
        assertThat(costs).hasSize(1);
        assertThat(costs.getFirst().getDocumentReference()).isEqualTo("INV-2024-001");
        assertThat(costs.getFirst().getDocumentStorageKey())
                .startsWith("costs/")
                .endsWith("/invoice.pdf");
    }
}
