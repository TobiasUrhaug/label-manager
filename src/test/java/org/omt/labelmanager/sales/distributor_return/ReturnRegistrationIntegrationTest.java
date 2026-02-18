package org.omt.labelmanager.sales.distributor_return;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.allocation.AllocationTestHelper;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnCommandApi;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReturnRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DistributorReturnCommandApi returnCommandApi;

    @Autowired
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private ProductionRunTestHelper productionRunTestHelper;

    @Autowired
    private AllocationTestHelper allocationTestHelper;

    @Autowired
    private DistributorQueryApi distributorQueryApi;

    private Long labelId;
    private Long distributorId;
    private Long releaseId;
    private Long productionRunId;

    @BeforeEach
    void setUp() {
        inventoryMovementRepository.deleteAll();

        var label = labelTestHelper.createLabelWithDirectDistributor("Test Label");
        labelId = label.id();

        distributorId = distributorQueryApi
                .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                .orElseThrow()
                .id();

        releaseId = releaseTestHelper.createReleaseEntity("Test Release", labelId);

        var productionRun = productionRunTestHelper.createProductionRun(
                releaseId, ReleaseFormat.VINYL, 100
        );
        productionRunId = productionRun.id();

        // Allocate 50 units to the distributor so they have inventory to return
        allocationTestHelper.createAllocation(productionRunId, distributorId, 50);
    }

    @Test
    void registerReturn_createsReturnWithLineItems() {
        var lineItems = List.of(
                new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 10)
        );

        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                "Monthly return",
                lineItems
        );

        assertThat(distributorReturn.id()).isNotNull();
        assertThat(distributorReturn.labelId()).isEqualTo(labelId);
        assertThat(distributorReturn.distributorId()).isEqualTo(distributorId);
        assertThat(distributorReturn.returnDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(distributorReturn.notes()).isEqualTo("Monthly return");
        assertThat(distributorReturn.lineItems()).hasSize(1);
        assertThat(distributorReturn.lineItems().getFirst().quantity()).isEqualTo(10);
    }

    @Test
    void registerReturn_createsReturnInventoryMovement() {
        var lineItems = List.of(
                new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 10)
        );

        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                lineItems
        );

        var returnMovements = inventoryMovementRepository
                .findByProductionRunIdOrderByOccurredAtDesc(productionRunId)
                .stream()
                .filter(m -> m.getMovementType() == MovementType.RETURN)
                .toList();

        assertThat(returnMovements).hasSize(1);
        assertThat(returnMovements.getFirst().getFromLocationType())
                .isEqualTo(LocationType.DISTRIBUTOR);
        assertThat(returnMovements.getFirst().getFromLocationId()).isEqualTo(distributorId);
        assertThat(returnMovements.getFirst().getToLocationType())
                .isEqualTo(LocationType.WAREHOUSE);
        assertThat(returnMovements.getFirst().getToLocationId()).isNull();
        assertThat(returnMovements.getFirst().getQuantity()).isEqualTo(10);
        assertThat(returnMovements.getFirst().getReferenceId()).isEqualTo(distributorReturn.id());
    }

    @Test
    void registerReturn_decreasesDistributorInventoryAndIncreasesWarehouse() {
        int distributorBefore = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, distributorId
        );
        int warehouseBefore = inventoryMovementQueryApi.getWarehouseInventory(productionRunId);

        var lineItems = List.of(
                new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 10)
        );

        returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                lineItems
        );

        int distributorAfter = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, distributorId
        );
        int warehouseAfter = inventoryMovementQueryApi.getWarehouseInventory(productionRunId);

        assertThat(distributorAfter).isEqualTo(distributorBefore - 10);
        assertThat(warehouseAfter).isEqualTo(warehouseBefore + 10);
    }

    @Test
    void registerReturn_withInsufficientInventory_throwsException() {
        var lineItems = List.of(
                new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 100) // more than 50 allocated
        );

        assertThatThrownBy(() -> returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                lineItems
        )).isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient inventory");
    }

    @Test
    void registerReturn_withEmptyLineItems_throwsException() {
        assertThatThrownBy(() -> returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one line item");
    }

    @Test
    void registerReturn_withNonExistentLabel_throwsException() {
        var lineItems = List.of(
                new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5)
        );

        assertThatThrownBy(() -> returnCommandApi.registerReturn(
                99999L, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                lineItems
        )).hasMessageContaining("Label not found");
    }

    @Test
    void registerReturn_withDistributorFromDifferentLabel_throwsException() {
        var otherLabel = labelTestHelper.createLabelWithDirectDistributor("Other Label");
        Long otherDistributorId = distributorQueryApi
                .findByLabelIdAndChannelType(otherLabel.id(), ChannelType.DIRECT)
                .orElseThrow()
                .id();

        var lineItems = List.of(
                new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5)
        );

        assertThatThrownBy(() -> returnCommandApi.registerReturn(
                labelId, otherDistributorId, // distributor from a different label
                LocalDate.of(2026, 2, 1),
                null,
                lineItems
        )).hasMessageContaining("not found for label");
    }
}
