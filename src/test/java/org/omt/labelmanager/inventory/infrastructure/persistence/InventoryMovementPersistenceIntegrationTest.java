package org.omt.labelmanager.inventory.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.label.LabelRepository;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.omt.labelmanager.inventory.domain.MovementType;
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
class InventoryMovementPersistenceIntegrationTest {

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
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private SalesChannelRepository salesChannelRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private LabelRepository labelRepository;

    private Long productionRunId;
    private Long salesChannelId;

    @BeforeEach
    void setUp() {
        inventoryMovementRepository.deleteAll();
        productionRunRepository.deleteAll();
        salesChannelRepository.deleteAll();
        releaseRepository.deleteAll();
        labelRepository.deleteAll();

        LabelEntity label = labelRepository.save(new LabelEntity("Test Label", null, null));
        ReleaseEntity release = releaseRepository.save(
                new ReleaseEntity(null, "Test Release", LocalDate.of(2025, 1, 1), label));

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        release.getId(),
                        ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        500
                ));
        productionRunId = productionRun.getId();

        SalesChannelEntity salesChannel = salesChannelRepository.save(
                new SalesChannelEntity(label.getId(), "Direct Sales", ChannelType.DIRECT));
        salesChannelId = salesChannel.getId();
    }

    @Test
    void savesAndRetrievesInventoryMovement() {
        Instant occurredAt = Instant.parse("2025-06-15T10:00:00Z");
        var entity = new InventoryMovementEntity(
                productionRunId,
                salesChannelId,
                100,
                MovementType.ALLOCATION,
                occurredAt,
                42L
        );

        var saved = inventoryMovementRepository.save(entity);

        var retrieved = inventoryMovementRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProductionRunId()).isEqualTo(productionRunId);
        assertThat(retrieved.get().getSalesChannelId()).isEqualTo(salesChannelId);
        assertThat(retrieved.get().getQuantityDelta()).isEqualTo(100);
        assertThat(retrieved.get().getMovementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(retrieved.get().getOccurredAt()).isEqualTo(occurredAt);
        assertThat(retrieved.get().getReferenceId()).isEqualTo(42L);
    }

    @Test
    void savesMovementWithNullReferenceId() {
        Instant occurredAt = Instant.now();
        var entity = new InventoryMovementEntity(
                productionRunId,
                salesChannelId,
                -50,
                MovementType.SALE,
                occurredAt,
                null
        );

        var saved = inventoryMovementRepository.save(entity);

        var retrieved = inventoryMovementRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getReferenceId()).isNull();
    }

    @Test
    void findsByProductionRunId() {
        Instant occurredAt = Instant.now();
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, salesChannelId, 100, MovementType.ALLOCATION, occurredAt, null));
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, salesChannelId, -20, MovementType.SALE, occurredAt, null));

        var movements = inventoryMovementRepository.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.getProductionRunId().equals(productionRunId));
    }

    @Test
    void findsBySalesChannelId() {
        Instant occurredAt = Instant.now();
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, salesChannelId, 100, MovementType.ALLOCATION, occurredAt, null));
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, salesChannelId, -20, MovementType.SALE, occurredAt, null));

        var movements = inventoryMovementRepository.findBySalesChannelId(salesChannelId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.getSalesChannelId().equals(salesChannelId));
    }

    @Test
    void deletesMovementWhenProductionRunDeleted() {
        Instant occurredAt = Instant.now();
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, salesChannelId, 100, MovementType.ALLOCATION, occurredAt, null));

        assertThat(inventoryMovementRepository.findByProductionRunId(productionRunId)).hasSize(1);

        productionRunRepository.deleteById(productionRunId);

        assertThat(inventoryMovementRepository.findByProductionRunId(productionRunId)).isEmpty();
    }
}
