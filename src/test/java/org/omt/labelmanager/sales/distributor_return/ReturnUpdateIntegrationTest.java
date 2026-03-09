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
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnCommandApi;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReturnUpdateIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DistributorReturnCommandApi returnCommandApi;

    @Autowired
    private DistributorReturnQueryApi returnQueryApi;

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

        // Allocate 50 units to the distributor
        allocationTestHelper.createAllocation(productionRunId, distributorId, 50);
    }

    @Test
    void updateReturn_updatesDateAndNotes() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                "Original notes",
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        returnCommandApi.updateReturn(
                distributorReturn.id(),
                LocalDate.of(2026, 2, 15),
                "Updated notes",
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        var updated = returnQueryApi.findById(distributorReturn.id()).orElseThrow();
        assertThat(updated.returnDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(updated.notes()).isEqualTo("Updated notes");
    }

    @Test
    void updateReturn_replacesLineItems() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        returnCommandApi.updateReturn(
                distributorReturn.id(),
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 20))
        );

        var updated = returnQueryApi.findById(distributorReturn.id()).orElseThrow();
        assertThat(updated.lineItems()).hasSize(1);
        assertThat(updated.lineItems().getFirst().quantity()).isEqualTo(20);
    }

    @Test
    void updateReturn_replacesMovements() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        returnCommandApi.updateReturn(
                distributorReturn.id(),
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 15))
        );

        var returnMovements = inventoryMovementRepository
                .findByProductionRunIdOrderByOccurredAtDesc(productionRunId)
                .stream()
                .filter(m -> m.getMovementType() == MovementType.RETURN)
                .toList();

        // Only one RETURN movement (the original was replaced)
        assertThat(returnMovements).hasSize(1);
        assertThat(returnMovements.getFirst().getQuantity()).isEqualTo(15);
        assertThat(returnMovements.getFirst().getReferenceId()).isEqualTo(distributorReturn.id());
    }

    @Test
    void updateReturn_restoresInventoryBeforeValidating_allowingLargerQuantity() {
        // Register a return of 10 — distributor now has 40 remaining
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 10))
        );

        // Update to 45 — would exceed distributor's apparent 40, but the original 10-unit
        // RETURN movement is reversed first, restoring 50 units, so 45 is valid
        returnCommandApi.updateReturn(
                distributorReturn.id(),
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 45))
        );

        int distributorInventory = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, distributorId
        );
        assertThat(distributorInventory).isEqualTo(5); // 50 allocated - 45 returned
    }

    @Test
    void updateReturn_withInsufficientInventory_throwsException() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        assertThatThrownBy(() -> returnCommandApi.updateReturn(
                distributorReturn.id(),
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 100)) // exceeds 50
        )).isInstanceOf(InsufficientInventoryException.class);
    }
}
