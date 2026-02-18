package org.omt.labelmanager.inventory.inventorymovement;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class RecordMovementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryMovementCommandApi inventoryMovementCommandApi;

    @Autowired
    private InventoryMovementQueryApi inventoryMovementQueryApi;

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
        productionRunRepository.deleteAll();
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId, ReleaseFormat.VINYL, "First pressing",
                        "Plant A", LocalDate.of(2025, 1, 1), 500));
        productionRunId = productionRun.getId();

        DistributorEntity distributor = distributorRepository.save(
                new DistributorEntity(label.id(), "Direct Sales", ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    @Test
    void recordMovement_persistsAllocationMovementWithAllFields() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                LocationType.WAREHOUSE, null,
                LocationType.DISTRIBUTOR, distributorId,
                100,
                MovementType.ALLOCATION,
                42L
        );

        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(1);
        var movement = movements.getFirst();
        assertThat(movement.productionRunId()).isEqualTo(productionRunId);
        assertThat(movement.fromLocationType()).isEqualTo(LocationType.WAREHOUSE);
        assertThat(movement.fromLocationId()).isNull();
        assertThat(movement.toLocationType()).isEqualTo(LocationType.DISTRIBUTOR);
        assertThat(movement.toLocationId()).isEqualTo(distributorId);
        assertThat(movement.quantity()).isEqualTo(100);
        assertThat(movement.movementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(movement.referenceId()).isEqualTo(42L);
        assertThat(movement.occurredAt()).isNotNull();
    }

    @Test
    void recordMovement_persistsSaleMovementWithNullReferenceId() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId,
                LocationType.EXTERNAL, null,
                50,
                MovementType.SALE,
                null
        );

        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(1);
        var movement = movements.getFirst();
        assertThat(movement.fromLocationType()).isEqualTo(LocationType.DISTRIBUTOR);
        assertThat(movement.fromLocationId()).isEqualTo(distributorId);
        assertThat(movement.toLocationType()).isEqualTo(LocationType.EXTERNAL);
        assertThat(movement.toLocationId()).isNull();
        assertThat(movement.quantity()).isEqualTo(50);
        assertThat(movement.referenceId()).isNull();
    }

    @Test
    void recordMovement_persistsMultipleMovements() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                LocationType.WAREHOUSE, null,
                LocationType.DISTRIBUTOR, distributorId,
                100, MovementType.ALLOCATION, null);
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId,
                LocationType.EXTERNAL, null,
                20, MovementType.SALE, null);

        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(2);
    }

    @Test
    void deleteMovementsByReference_removesMatchingMovements() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId,
                LocationType.EXTERNAL, null,
                30, MovementType.SALE, 99L);
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId,
                LocationType.EXTERNAL, null,
                20, MovementType.SALE, 99L);
        // Different referenceId — should NOT be deleted
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId,
                LocationType.EXTERNAL, null,
                10, MovementType.SALE, 100L);

        inventoryMovementCommandApi.deleteMovementsByReference(MovementType.SALE, 99L);

        var remaining = inventoryMovementQueryApi.findByProductionRunId(productionRunId);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().referenceId()).isEqualTo(100L);
    }
}
