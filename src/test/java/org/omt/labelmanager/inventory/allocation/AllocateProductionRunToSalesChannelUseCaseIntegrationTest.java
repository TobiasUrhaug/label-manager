package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.inventory.allocation.application.CreateAllocationUseCase;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.domain.InsufficientInventoryException;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
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
class AllocateProductionRunToDistributorUseCaseIntegrationTest {

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
    private CreateAllocationUseCase allocateProductionRunToDistributorUseCase;

    @Autowired
    private ChannelAllocationRepository channelAllocationRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private LabelTestHelper labelTestHelper;

    private Long productionRunId;
    private Long distributorId;

    @BeforeEach
    void setUp() {
        channelAllocationRepository.deleteAll();
        inventoryMovementRepository.deleteAll();
        productionRunRepository.deleteAll();
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId,
                        ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        500
                ));
        productionRunId = productionRun.getId();

        DistributorEntity distributor = distributorRepository.save(
                new DistributorEntity(label.id(), "Direct Sales", ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    @Test
    void createsAllocationAndMovement() {
        var allocation = allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 100);

        assertThat(allocation.id()).isNotNull();
        assertThat(allocation.productionRunId()).isEqualTo(productionRunId);
        assertThat(allocation.distributorId()).isEqualTo(distributorId);
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
        allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 200);
        allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 200);
        var thirdAllocation = allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 100);

        assertThat(thirdAllocation.quantity()).isEqualTo(100);
        assertThat(channelAllocationRepository.sumQuantityByProductionRunId(productionRunId))
                .isEqualTo(500);
    }

    @Test
    void throwsExceptionWhenOverAllocating() {
        allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 400);

        assertThatThrownBy(() ->
                allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 200))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("requested 200")
                .hasMessageContaining("only 100 available");
    }

    @Test
    void throwsExceptionWhenAllocatingMoreThanTotal() {
        assertThatThrownBy(() ->
                allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 600))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("requested 600")
                .hasMessageContaining("only 500 available");
    }

    @Test
    void exceptionContainsRequestedAndAvailableQuantities() {
        allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 450);

        try {
            allocateProductionRunToDistributorUseCase.execute(productionRunId, distributorId, 100);
        } catch (InsufficientInventoryException ex) {
            assertThat(ex.getRequested()).isEqualTo(100);
            assertThat(ex.getAvailable()).isEqualTo(50);
        }
    }
}
