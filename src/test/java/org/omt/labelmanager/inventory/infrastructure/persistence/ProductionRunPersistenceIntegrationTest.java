package org.omt.labelmanager.inventory.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
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
class ProductionRunPersistenceIntegrationTest {

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
    void savesAndRetrievesProductionRun() {
        var entity = new ProductionRunEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        var saved = productionRunRepository.save(entity);

        var retrieved = productionRunRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getReleaseId()).isEqualTo(releaseId);
        assertThat(retrieved.get().getFormat()).isEqualTo(ReleaseFormat.VINYL);
        assertThat(retrieved.get().getDescription()).isEqualTo("Original pressing");
        assertThat(retrieved.get().getManufacturer()).isEqualTo("Record Industry");
        assertThat(retrieved.get().getManufacturingDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(retrieved.get().getQuantity()).isEqualTo(500);
    }

    @Test
    void findsByReleaseId() {
        productionRunRepository.save(new ProductionRunEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        ));

        productionRunRepository.save(new ProductionRunEntity(
                releaseId,
                ReleaseFormat.CD,
                "Initial run",
                "CD Plant",
                LocalDate.of(2025, 1, 15),
                200
        ));

        LabelEntity otherLabel = labelRepository.save(
                new LabelEntity("Other Label", null, null));

        ReleaseEntity otherRelease = new ReleaseEntity(
                null,
                "Other Release",
                LocalDate.of(2025, 2, 1),
                otherLabel
        );
        otherRelease = releaseRepository.save(otherRelease);

        productionRunRepository.save(new ProductionRunEntity(
                otherRelease.getId(),
                ReleaseFormat.CASSETTE,
                "Limited edition",
                "Tape Factory",
                LocalDate.of(2025, 2, 1),
                100
        ));

        var productionRunsForRelease = productionRunRepository.findByReleaseId(releaseId);

        assertThat(productionRunsForRelease).hasSize(2);
        assertThat(productionRunsForRelease)
                .allMatch(pr -> pr.getReleaseId().equals(releaseId));
    }

    @Test
    void deletesProductionRunWhenReleaseDeleted() {
        productionRunRepository.save(new ProductionRunEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        ));

        assertThat(productionRunRepository.findByReleaseId(releaseId)).hasSize(1);

        releaseRepository.deleteById(releaseId);

        assertThat(productionRunRepository.findByReleaseId(releaseId)).isEmpty();
    }
}
