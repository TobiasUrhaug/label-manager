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
    void recordMovement_persistsMovementWithAllFields() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                distributorId,
                100,
                MovementType.ALLOCATION,
                42L
        );

        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(1);
        var movement = movements.getFirst();
        assertThat(movement.productionRunId()).isEqualTo(productionRunId);
        assertThat(movement.distributorId()).isEqualTo(distributorId);
        assertThat(movement.quantityDelta()).isEqualTo(100);
        assertThat(movement.movementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(movement.referenceId()).isEqualTo(42L);
        assertThat(movement.occurredAt()).isNotNull();
    }

    @Test
    void recordMovement_persistsMovementWithNullReferenceId() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                distributorId,
                -50,
                MovementType.SALE,
                null
        );

        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(1);
        var movement = movements.getFirst();
        assertThat(movement.referenceId()).isNull();
    }

    @Test
    void recordMovement_persistsMultipleMovements() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId, distributorId, 100, MovementType.ALLOCATION, null);
        inventoryMovementCommandApi.recordMovement(
                productionRunId, distributorId, -20, MovementType.SALE, null);

        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(2);
    }
}
