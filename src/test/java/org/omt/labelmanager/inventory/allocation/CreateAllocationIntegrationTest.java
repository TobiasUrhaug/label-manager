package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.domain.InsufficientInventoryException;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateAllocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AllocationCommandApi allocationCommandApi;

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
        var allocation = allocationCommandApi.createAllocation(productionRunId, distributorId, 100);

        assertThat(allocation.id()).isNotNull();
        assertThat(allocation.productionRunId()).isEqualTo(productionRunId);
        assertThat(allocation.distributorId()).isEqualTo(distributorId);
        assertThat(allocation.quantity()).isEqualTo(100);
        assertThat(allocation.allocatedAt()).isNotNull();

        var movements = inventoryMovementRepository
                .findByProductionRunIdOrderByOccurredAtDesc(productionRunId);
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getFromLocationType()).isEqualTo(
                org.omt.labelmanager.inventory.domain.LocationType.WAREHOUSE);
        assertThat(movements.get(0).getToLocationType()).isEqualTo(
                org.omt.labelmanager.inventory.domain.LocationType.DISTRIBUTOR);
        assertThat(movements.get(0).getToLocationId()).isEqualTo(distributorId);
        assertThat(movements.get(0).getQuantity()).isEqualTo(100);
        assertThat(movements.get(0).getMovementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(movements.get(0).getReferenceId()).isEqualTo(allocation.id());
    }

    @Test
    void allowsMultipleAllocationsUpToManufacturedQuantity() {
        allocationCommandApi.createAllocation(productionRunId, distributorId, 200);
        allocationCommandApi.createAllocation(productionRunId, distributorId, 200);
        var thirdAllocation = allocationCommandApi.createAllocation(productionRunId, distributorId, 100);

        assertThat(thirdAllocation.quantity()).isEqualTo(100);
        assertThat(channelAllocationRepository.sumQuantityByProductionRunId(productionRunId))
                .isEqualTo(500);
    }

    @Test
    void throwsExceptionWhenOverAllocating() {
        allocationCommandApi.createAllocation(productionRunId, distributorId, 400);

        assertThatThrownBy(() ->
                allocationCommandApi.createAllocation(productionRunId, distributorId, 200))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("requested 200")
                .hasMessageContaining("only 100 available");
    }

    @Test
    void throwsExceptionWhenAllocatingMoreThanTotal() {
        assertThatThrownBy(() ->
                allocationCommandApi.createAllocation(productionRunId, distributorId, 600))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("requested 600")
                .hasMessageContaining("only 500 available");
    }

    @Test
    void exceptionContainsRequestedAndAvailableQuantities() {
        allocationCommandApi.createAllocation(productionRunId, distributorId, 450);

        try {
            allocationCommandApi.createAllocation(productionRunId, distributorId, 100);
        } catch (InsufficientInventoryException ex) {
            assertThat(ex.getRequested()).isEqualTo(100);
            assertThat(ex.getAvailable()).isEqualTo(50);
        }
    }
}
