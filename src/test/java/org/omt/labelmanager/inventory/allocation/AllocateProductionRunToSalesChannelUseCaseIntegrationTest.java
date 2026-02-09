package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.release.ReleaseRepository;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.*;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AllocateProductionRunToSalesChannelUseCaseIntegrationTest {

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
    private AllocateProductionRunToSalesChannelUseCase allocateProductionRunToSalesChannelUseCase;

    @Autowired
    private ChannelAllocationRepository channelAllocationRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private SalesChannelRepository salesChannelRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    private Long productionRunId;
    private Long salesChannelId;

    @BeforeEach
    void setUp() {
        channelAllocationRepository.deleteAll();
        inventoryMovementRepository.deleteAll();
        productionRunRepository.deleteAll();
        salesChannelRepository.deleteAll();
        releaseRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        ReleaseEntity release = releaseRepository.save(
                new ReleaseEntity(
                        null,
                        "Test Release",
                        LocalDate.of(2025, 1, 1),
                        label.id()
                ));

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
                new SalesChannelEntity(label.id(), "Direct Sales", ChannelType.DIRECT));
        salesChannelId = salesChannel.getId();
    }

    @Test
    void createsAllocationAndMovement() {
        var allocation = allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 100);

        assertThat(allocation.id()).isNotNull();
        assertThat(allocation.productionRunId()).isEqualTo(productionRunId);
        assertThat(allocation.salesChannelId()).isEqualTo(salesChannelId);
        assertThat(allocation.quantity()).isEqualTo(100);
        assertThat(allocation.allocatedAt()).isNotNull();

        var movements = inventoryMovementRepository.findByProductionRunId(productionRunId);
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getQuantityDelta()).isEqualTo(100);
        assertThat(movements.get(0).getMovementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(movements.get(0).getReferenceId()).isEqualTo(allocation.id());
    }

    @Test
    void allowsMultipleAllocationsUpToManufacturedQuantity() {
        allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 200);
        allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 200);
        var thirdAllocation = allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 100);

        assertThat(thirdAllocation.quantity()).isEqualTo(100);
        assertThat(channelAllocationRepository.sumQuantityByProductionRunId(productionRunId))
                .isEqualTo(500);
    }

    @Test
    void throwsExceptionWhenOverAllocating() {
        allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 400);

        assertThatThrownBy(() ->
                allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 200))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("requested 200")
                .hasMessageContaining("only 100 available");
    }

    @Test
    void throwsExceptionWhenAllocatingMoreThanTotal() {
        assertThatThrownBy(() ->
                allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 600))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("requested 600")
                .hasMessageContaining("only 500 available");
    }

    @Test
    void exceptionContainsRequestedAndAvailableQuantities() {
        allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 450);

        try {
            allocateProductionRunToSalesChannelUseCase.invoke(productionRunId, salesChannelId, 100);
        } catch (InsufficientInventoryException ex) {
            assertThat(ex.getRequested()).isEqualTo(100);
            assertThat(ex.getAvailable()).isEqualTo(50);
        }
    }
}
