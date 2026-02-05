package org.omt.labelmanager.productionrun.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
import org.omt.labelmanager.productionrun.infrastructure.persistence.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductionRunCRUDHandlerIntegrationTest {

    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";

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

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("storage.s3.endpoint", minIO::getS3URL);
        registry.add("storage.s3.bucket", () -> "costs");
        registry.add("storage.s3.region", () -> "us-east-1");
        registry.add("storage.s3.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("storage.s3.secret-key", () -> MINIO_SECRET_KEY);
    }

    @Autowired
    private ProductionRunCRUDHandler productionRunCRUDHandler;

    @Autowired
    private ProductionRunQueryService productionRunQueryService;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private LabelRepository labelRepository;

    private Long releaseId;

    @BeforeEach
    void setUp() {
        productionRunRepository.deleteAll();
        releaseRepository.deleteAll();
        labelRepository.deleteAll();

        LabelEntity label = labelRepository.save(new LabelEntity("Test Label", null, null));

        ReleaseEntity release = new ReleaseEntity(
                null,
                "Test Release",
                LocalDate.of(2025, 1, 1),
                label
        );
        release = releaseRepository.save(release);
        releaseId = release.getId();
    }

    @Test
    void createsProductionRun() {
        var productionRun = productionRunCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(productionRun.id()).isNotNull();
        assertThat(productionRun.releaseId()).isEqualTo(releaseId);
        assertThat(productionRun.format()).isEqualTo(ReleaseFormat.VINYL);
        assertThat(productionRun.description()).isEqualTo("Original pressing");
        assertThat(productionRun.manufacturer()).isEqualTo("Record Industry");
        assertThat(productionRun.manufacturingDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(productionRun.quantity()).isEqualTo(500);
    }

    @Test
    void findsProductionRunsByReleaseId() {
        productionRunCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        productionRunCRUDHandler.create(
                releaseId,
                ReleaseFormat.CD,
                "Initial run",
                "CD Plant",
                LocalDate.of(2025, 1, 15),
                200
        );

        var productionRuns = productionRunCRUDHandler.findByReleaseId(releaseId);

        assertThat(productionRuns).hasSize(2);
    }

    @Test
    void deletesProductionRun() {
        var productionRun = productionRunCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        boolean deleted = productionRunCRUDHandler.delete(productionRun.id());

        assertThat(deleted).isTrue();
        assertThat(productionRunCRUDHandler.findByReleaseId(releaseId)).isEmpty();
    }

    @Test
    void deleteReturnsFalseForNonExistentProductionRun() {
        boolean deleted = productionRunCRUDHandler.delete(999L);

        assertThat(deleted).isFalse();
    }

}
