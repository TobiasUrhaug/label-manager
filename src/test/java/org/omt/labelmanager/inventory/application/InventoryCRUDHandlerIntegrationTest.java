package org.omt.labelmanager.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryRepository;
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
class InventoryCRUDHandlerIntegrationTest {

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
    private InventoryCRUDHandler inventoryCRUDHandler;

    @Autowired
    private InventoryQueryService inventoryQueryService;

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
    void createsInventory() {
        var inventory = inventoryCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        assertThat(inventory.id()).isNotNull();
        assertThat(inventory.releaseId()).isEqualTo(releaseId);
        assertThat(inventory.format()).isEqualTo(ReleaseFormat.VINYL);
        assertThat(inventory.description()).isEqualTo("Original pressing");
        assertThat(inventory.manufacturer()).isEqualTo("Record Industry");
        assertThat(inventory.manufacturingDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(inventory.quantity()).isEqualTo(500);
    }

    @Test
    void findsInventoryByReleaseId() {
        inventoryCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        inventoryCRUDHandler.create(
                releaseId,
                ReleaseFormat.CD,
                "Initial run",
                "CD Plant",
                LocalDate.of(2025, 1, 15),
                200
        );

        var inventoryList = inventoryCRUDHandler.findByReleaseId(releaseId);

        assertThat(inventoryList).hasSize(2);
    }

    @Test
    void deletesInventory() {
        var inventory = inventoryCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        boolean deleted = inventoryCRUDHandler.delete(inventory.id());

        assertThat(deleted).isTrue();
        assertThat(inventoryCRUDHandler.findByReleaseId(releaseId)).isEmpty();
    }

    @Test
    void deleteReturnsFalseForNonExistentInventory() {
        boolean deleted = inventoryCRUDHandler.delete(999L);

        assertThat(deleted).isFalse();
    }

    @Test
    void calculatesTotalsByFormat() {
        inventoryCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "Original pressing",
                "Record Industry",
                LocalDate.of(2025, 1, 1),
                500
        );

        inventoryCRUDHandler.create(
                releaseId,
                ReleaseFormat.VINYL,
                "2nd pressing",
                "Record Industry",
                LocalDate.of(2025, 6, 1),
                300
        );

        inventoryCRUDHandler.create(
                releaseId,
                ReleaseFormat.CD,
                "Initial run",
                "CD Plant",
                LocalDate.of(2025, 1, 15),
                200
        );

        var totals = inventoryQueryService.getTotalsForRelease(releaseId);

        assertThat(totals).containsEntry(ReleaseFormat.VINYL, 800);
        assertThat(totals).containsEntry(ReleaseFormat.CD, 200);
        assertThat(totals).hasSize(2);
    }
}
