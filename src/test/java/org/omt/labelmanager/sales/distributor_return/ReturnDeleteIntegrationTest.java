package org.omt.labelmanager.sales.distributor_return;

import jakarta.persistence.EntityNotFoundException;
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

class ReturnDeleteIntegrationTest extends AbstractIntegrationTest {

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

        allocationTestHelper.createAllocation(productionRunId, distributorId, 50);
    }

    @Test
    void deleteReturn_removesReturnFromDatabase() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 10))
        );

        returnCommandApi.deleteReturn(distributorReturn.id());

        assertThat(returnQueryApi.findById(distributorReturn.id())).isEmpty();
    }

    @Test
    void deleteReturn_restoresDistributorInventory() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 10))
        );

        // After registration, distributor has 40 units
        assertThat(inventoryMovementQueryApi.getCurrentInventory(productionRunId, distributorId))
                .isEqualTo(40);

        returnCommandApi.deleteReturn(distributorReturn.id());

        // After deletion, RETURN movement is gone, distributor has 50 again
        assertThat(inventoryMovementQueryApi.getCurrentInventory(productionRunId, distributorId))
                .isEqualTo(50);
    }

    @Test
    void deleteReturn_removesReturnMovements() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 10))
        );

        returnCommandApi.deleteReturn(distributorReturn.id());

        var remainingReturnMovements = inventoryMovementRepository
                .findByProductionRunIdOrderByOccurredAtDesc(productionRunId)
                .stream()
                .filter(m -> m.getMovementType() == MovementType.RETURN
                        && distributorReturn.id().equals(m.getReferenceId()))
                .toList();

        assertThat(remainingReturnMovements).isEmpty();
    }

    @Test
    void deleteReturn_withNonExistentId_throwsException() {
        assertThatThrownBy(() -> returnCommandApi.deleteReturn(99999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Return not found");
    }
}
