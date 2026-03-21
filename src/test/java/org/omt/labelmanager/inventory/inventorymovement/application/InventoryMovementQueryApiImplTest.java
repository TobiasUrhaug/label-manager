package org.omt.labelmanager.inventory.inventorymovement.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorRepository;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementEntity;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryMovementQueryApiImplTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    private Long productionRunId;
    private Long distributorId;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        productionRunRepository.deleteAll();
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        ProductionRunEntity run = productionRunRepository.save(new ProductionRunEntity(
                releaseId, ReleaseFormat.VINYL, "First pressing", "Plant A",
                LocalDate.of(2025, 1, 1), 500));
        productionRunId = run.getId();

        DistributorEntity distributor = distributorRepository.save(
                new DistributorEntity(label.id(), "Test Distro", ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    // --- getBandcampInventory ---

    @Test
    void getBandcampInventory_returnsNetHeld_afterAllocationAndSale() {
        saveMovement(productionRunId, LocationType.WAREHOUSE, null, LocationType.BANDCAMP, null, 50, MovementType.ALLOCATION);
        saveMovement(productionRunId, LocationType.BANDCAMP, null, LocationType.EXTERNAL, null, 10, MovementType.SALE);

        assertThat(inventoryMovementQueryApi.getBandcampInventory(productionRunId)).isEqualTo(40);
    }

    @Test
    void getBandcampInventory_returnsNetHeld_afterAllocationSaleAndReturn() {
        saveMovement(productionRunId, LocationType.WAREHOUSE, null, LocationType.BANDCAMP, null, 50, MovementType.ALLOCATION);
        saveMovement(productionRunId, LocationType.BANDCAMP, null, LocationType.EXTERNAL, null, 10, MovementType.SALE);
        saveMovement(productionRunId, LocationType.BANDCAMP, null, LocationType.WAREHOUSE, null, 10, MovementType.RETURN);

        assertThat(inventoryMovementQueryApi.getBandcampInventory(productionRunId)).isEqualTo(30);
    }

    // --- getProductionRunIdsAllocatedToDistributor ---

    @Test
    void getProductionRunIdsAllocatedToDistributor_returnsBothRunIds_whenEachHasAllocationToDistributor() {
        var label = labelTestHelper.createLabel("Label 2");
        Long releaseId2 = releaseTestHelper.createReleaseEntity("Release 2", label.id());
        Long runId2 = productionRunRepository.save(new ProductionRunEntity(
                releaseId2, ReleaseFormat.VINYL, "Second pressing", "Plant B",
                LocalDate.of(2025, 6, 1), 300)).getId();

        saveMovement(productionRunId, LocationType.WAREHOUSE, null, LocationType.DISTRIBUTOR, distributorId, 100, MovementType.ALLOCATION);
        saveMovement(runId2, LocationType.WAREHOUSE, null, LocationType.DISTRIBUTOR, distributorId, 50, MovementType.ALLOCATION);

        var result = inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(distributorId);

        assertThat(result).containsExactlyInAnyOrder(productionRunId, runId2);
    }

    @Test
    void getProductionRunIdsAllocatedToDistributor_excludesRun_whenOnlySaleWithoutAllocation() {
        saveMovement(productionRunId, LocationType.DISTRIBUTOR, distributorId, LocationType.EXTERNAL, null, 20, MovementType.SALE);

        var result = inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(distributorId);

        assertThat(result).isEmpty();
    }

    private void saveMovement(
            Long runId,
            LocationType fromType, Long fromId,
            LocationType toType, Long toId,
            int quantity, MovementType movementType
    ) {
        movementRepository.save(new InventoryMovementEntity(
                runId, fromType, fromId, toType, toId, quantity, movementType, Instant.now(), null));
    }
}
