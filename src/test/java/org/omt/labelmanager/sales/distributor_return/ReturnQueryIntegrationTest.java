package org.omt.labelmanager.sales.distributor_return;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.DistributorTestHelper;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.allocation.AllocationTestHelper;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnCommandApi;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DistributorReturnCommandApi returnCommandApi;

    @Autowired
    private DistributorReturnQueryApi returnQueryApi;

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

    @Autowired
    private DistributorTestHelper distributorTestHelper;

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
    void findById_returnsReturnWithLineItems() {
        var distributorReturn = returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                "Test notes",
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        var found = returnQueryApi.findById(distributorReturn.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(distributorReturn.id());
        assertThat(found.get().lineItems()).hasSize(1);
        assertThat(found.get().lineItems().getFirst().releaseId()).isEqualTo(releaseId);
        assertThat(found.get().lineItems().getFirst().quantity()).isEqualTo(5);
    }

    @Test
    void findById_returnsEmptyForNonExistentId() {
        assertThat(returnQueryApi.findById(99999L)).isEmpty();
    }

    @Test
    void getReturnsForLabel_returnsSortedByReturnDateDescending() {
        returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 1, 1), // older
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );
        returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1), // newer
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        var returns = returnQueryApi.getReturnsForLabel(labelId);

        assertThat(returns).hasSize(2);
        assertThat(returns.get(0).returnDate())
                .isEqualTo(LocalDate.of(2026, 2, 1)); // newest first
        assertThat(returns.get(1).returnDate())
                .isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void getReturnsForLabel_returnsOnlyReturnsForThatLabel() {
        // Register return for test label
        returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        // Create another label with its own return
        var otherLabel = labelTestHelper.createLabelWithDirectDistributor("Other Label");
        Long otherDistributorId = distributorQueryApi
                .findByLabelIdAndChannelType(otherLabel.id(), ChannelType.DIRECT)
                .orElseThrow().id();
        Long otherReleaseId = releaseTestHelper.createReleaseEntity("Other Release", otherLabel.id());
        var otherProductionRun = productionRunTestHelper.createProductionRun(
                otherReleaseId, ReleaseFormat.VINYL, 100
        );
        allocationTestHelper.createAllocation(otherProductionRun.id(), otherDistributorId, 20);
        returnCommandApi.registerReturn(
                otherLabel.id(), otherDistributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(otherReleaseId, ReleaseFormat.VINYL, 5))
        );

        var returns = returnQueryApi.getReturnsForLabel(labelId);

        assertThat(returns).hasSize(1);
        assertThat(returns.getFirst().labelId()).isEqualTo(labelId);
    }

    @Test
    void getReturnsForDistributor_returnsOnlyReturnsForThatDistributor() {
        returnCommandApi.registerReturn(
                labelId, distributorId,
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        // Create a second distributor with allocation and return
        var otherDistributor = distributorTestHelper.createDistributor(
                labelId, "Other Distributor", ChannelType.DISTRIBUTOR
        );
        allocationTestHelper.createAllocation(productionRunId, otherDistributor.id(), 20);
        returnCommandApi.registerReturn(
                labelId, otherDistributor.id(),
                LocalDate.of(2026, 2, 1),
                null,
                List.of(new ReturnLineItemInput(releaseId, ReleaseFormat.VINYL, 5))
        );

        var returnsForDistributor = returnQueryApi.getReturnsForDistributor(distributorId);

        assertThat(returnsForDistributor).hasSize(1);
        assertThat(returnsForDistributor.getFirst().distributorId()).isEqualTo(distributorId);
    }
}
