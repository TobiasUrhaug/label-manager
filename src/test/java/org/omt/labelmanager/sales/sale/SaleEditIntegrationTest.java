package org.omt.labelmanager.sales.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SaleEditIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SaleCommandApi saleCommandApi;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private ProductionRunTestHelper productionRunTestHelper;

    @Autowired
    private DistributorQueryApi distributorQueryApi;

    @Autowired
    private AllocationCommandApi allocationCommandApi;

    @Autowired
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    private Long labelId;
    private Long releaseId;
    private Long productionRunId;
    private Long directDistributorId;

    @BeforeEach
    void setUp() {
        var label = labelTestHelper.createLabelWithDirectDistributor("Edit Test Label");
        labelId = label.id();

        directDistributorId = distributorQueryApi
                .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                .orElseThrow()
                .id();

        releaseId = releaseTestHelper.createReleaseEntity("Edit Test Release", labelId);

        var productionRun = productionRunTestHelper.createProductionRun(
                releaseId, ReleaseFormat.VINYL, "First pressing", "Plant A",
                LocalDate.of(2025, 1, 1), 100
        );
        productionRunId = productionRun.id();

        allocationCommandApi.createAllocation(productionRunId, directDistributorId, 80);
    }

    @Test
    void updateSale_updatesDateNotesAndLineItems() {
        var original = registerDirectSale(5, LocalDate.of(2026, 1, 10));

        var updated = saleCommandApi.updateSale(
                original.id(),
                LocalDate.of(2026, 5, 1),
                "Updated notes",
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, 8,
                        Money.of(new BigDecimal("20.00"))))
        );

        assertThat(updated.saleDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(updated.notes()).isEqualTo("Updated notes");
        assertThat(updated.lineItems()).hasSize(1);
        assertThat(updated.lineItems().getFirst().quantity()).isEqualTo(8);
        assertThat(updated.totalAmount().amount())
                .isEqualByComparingTo(new BigDecimal("160.00")); // 8 × 20
    }

    @Test
    void updateSale_replacesOldMovementsWithNewOnes() {
        var original = registerDirectSale(10, LocalDate.of(2026, 2, 1));
        // After selling 10: current inventory = 70

        saleCommandApi.updateSale(
                original.id(),
                original.saleDate(),
                null,
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, 20,
                        Money.of(new BigDecimal("15.00"))))
        );

        // Old movements deleted + new ones recorded: 80 - 20 = 60
        int currentInventory = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, directDistributorId
        );
        assertThat(currentInventory).isEqualTo(60);
    }

    @Test
    void updateSale_restoresInventoryBeforeValidating_allowingLargerQuantity() {
        var original = registerDirectSale(10, LocalDate.of(2026, 2, 1));
        // After selling 10: current inventory = 70; without restoration, 75 would fail.
        // But old movements are deleted first, restoring inventory to 80, so 75 is valid.

        var updated = saleCommandApi.updateSale(
                original.id(),
                original.saleDate(),
                null,
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, 75,
                        Money.of(new BigDecimal("15.00"))))
        );

        assertThat(updated.lineItems().getFirst().quantity()).isEqualTo(75);
        assertThat(inventoryMovementQueryApi
                .getCurrentInventory(productionRunId, directDistributorId))
                .isEqualTo(5); // 80 - 75
    }

    @Test
    void updateSale_withInsufficientInventory_throwsException() {
        var original = registerDirectSale(5, LocalDate.of(2026, 2, 1));

        assertThatThrownBy(() -> saleCommandApi.updateSale(
                original.id(),
                original.saleDate(),
                null,
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, 200, // more than the 80 available
                        Money.of(new BigDecimal("15.00"))))
        )).isInstanceOf(InsufficientInventoryException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Sale registerDirectSale(int quantity, LocalDate date) {
        return saleCommandApi.registerSale(
                labelId, date, ChannelType.DIRECT, null, null,
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, quantity,
                        Money.of(new BigDecimal("15.00"))))
        );
    }
}
