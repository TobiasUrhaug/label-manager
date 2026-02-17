package org.omt.labelmanager.sales.sale;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.domain.InventoryMovement;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SaleDeleteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SaleCommandApi saleCommandApi;

    @Autowired
    private SaleQueryApi saleQueryApi;

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
        var label = labelTestHelper.createLabelWithDirectDistributor("Delete Test Label");
        labelId = label.id();

        directDistributorId = distributorQueryApi
                .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                .orElseThrow()
                .id();

        releaseId = releaseTestHelper.createReleaseEntity("Delete Test Release", labelId);

        var productionRun = productionRunTestHelper.createProductionRun(
                releaseId, ReleaseFormat.VINYL, "First pressing", "Plant A",
                LocalDate.of(2025, 1, 1), 100
        );
        productionRunId = productionRun.id();

        allocationCommandApi.createAllocation(productionRunId, directDistributorId, 80);
    }

    @Test
    void deleteSale_removesSaleFromDatabase() {
        var sale = registerDirectSale(5);

        saleCommandApi.deleteSale(sale.id());

        assertThat(saleQueryApi.findById(sale.id())).isEmpty();
    }

    @Test
    void deleteSale_reversesInventoryMovements() {
        var sale = registerDirectSale(10);
        assertThat(inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, directDistributorId)).isEqualTo(70); // 80 - 10

        saleCommandApi.deleteSale(sale.id());

        assertThat(inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, directDistributorId)).isEqualTo(80); // fully restored
    }

    @Test
    void deleteSale_removesSaleMovements() {
        var sale = registerDirectSale(5);

        saleCommandApi.deleteSale(sale.id());

        List<InventoryMovement> movements =
                inventoryMovementQueryApi.getMovementsForProductionRun(productionRunId);
        boolean hasSaleMovement = movements.stream()
                .anyMatch(m -> m.movementType() == MovementType.SALE
                        && m.referenceId().equals(sale.id()));
        assertThat(hasSaleMovement).isFalse();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Sale registerDirectSale(int quantity) {
        return saleCommandApi.registerSale(
                labelId, LocalDate.of(2026, 2, 1), ChannelType.DIRECT, null, null,
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, quantity,
                        Money.of(new BigDecimal("15.00"))))
        );
    }
}
