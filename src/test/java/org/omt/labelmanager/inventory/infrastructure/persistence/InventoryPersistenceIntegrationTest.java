package org.omt.labelmanager.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryPersistenceIntegrationTest {

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
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private LabelRepository labelRepository;

    private Long releaseId;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
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
    void savesAndRetrievesInventory() {
        var entity = new InventoryEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        var saved = inventoryRepository.save(entity);

        var retrieved = inventoryRepository.findById(saved.getId());
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
        inventoryRepository.save(new InventoryEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        ));

        inventoryRepository.save(new InventoryEntity(
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

        inventoryRepository.save(new InventoryEntity(
                otherRelease.getId(),
                ReleaseFormat.CASSETTE,
                "Limited edition",
                "Tape Factory",
                LocalDate.of(2025, 2, 1),
                100
        ));

        var inventoryForRelease = inventoryRepository.findByReleaseId(releaseId);

        assertThat(inventoryForRelease).hasSize(2);
        assertThat(inventoryForRelease)
                .allMatch(inv -> inv.getReleaseId().equals(releaseId));
    }

    @Test
    void deletesInventoryWhenReleaseDeleted() {
        inventoryRepository.save(new InventoryEntity(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        ));

        assertThat(inventoryRepository.findByReleaseId(releaseId)).hasSize(1);

        releaseRepository.deleteById(releaseId);

        assertThat(inventoryRepository.findByReleaseId(releaseId)).isEmpty();
    }
}
