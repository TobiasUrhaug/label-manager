package org.omt.labelmanager.sales.sale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SaleQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SaleCommandApi saleCommandApi;

    @Autowired
    private SaleQueryApi saleQueryApi;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private ChannelAllocationRepository channelAllocationRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private AllocationCommandApi allocationCommandApi;

    private Long labelId;
    private Long releaseId;
    private Long productionRunId;
    private Long directDistributorId;
    private Long externalDistributorId;

    @BeforeEach
    void setUp() {
        inventoryMovementRepository.deleteAll();
        channelAllocationRepository.deleteAll();
        productionRunRepository.deleteAll();
        saleRepository.deleteAll();

        var label = labelTestHelper.createLabelWithDirectDistributor("Query Test Label");
        labelId = label.id();

        directDistributorId = distributorRepository
                .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                .orElseThrow()
                .getId();

        var externalDistributor = distributorRepository.save(
                new DistributorEntity(labelId, "External Distributor", ChannelType.DISTRIBUTOR)
        );
        externalDistributorId = externalDistributor.getId();

        releaseId = releaseTestHelper.createReleaseEntity("Query Test Release", labelId);

        var productionRun = productionRunRepository.save(new ProductionRunEntity(
                releaseId, ReleaseFormat.VINYL, "First pressing", "Plant A",
                LocalDate.of(2025, 1, 1), 200
        ));
        productionRunId = productionRun.getId();

        allocationCommandApi.createAllocation(productionRunId, directDistributorId, 80);
        allocationCommandApi.createAllocation(productionRunId, externalDistributorId, 80);
    }

    // ── distributorId is persisted on registration ──────────────────────────

    @Test
    void registerSale_persistsDistributorId() {
        var sale = registerDirectSale(5);

        assertThat(sale.distributorId()).isEqualTo(directDistributorId);
    }

    @Test
    void registerDistributorSale_persistsCorrectDistributorId() {
        var sale = registerDistributorSale(externalDistributorId, 5);

        assertThat(sale.distributorId()).isEqualTo(externalDistributorId);
    }

    // ── getSalesForDistributor ───────────────────────────────────────────────

    @Test
    void getSalesForDistributor_returnsOnlySalesForThatDistributor() {
        registerDirectSale(5);
        registerDirectSale(3);
        registerDistributorSale(externalDistributorId, 10);

        List<Sale> directSales = saleQueryApi.getSalesForDistributor(directDistributorId);
        List<Sale> externalSales = saleQueryApi.getSalesForDistributor(externalDistributorId);

        assertThat(directSales).hasSize(2);
        assertThat(directSales).allMatch(s -> s.distributorId().equals(directDistributorId));

        assertThat(externalSales).hasSize(1);
        assertThat(externalSales.getFirst().distributorId()).isEqualTo(externalDistributorId);
    }

    @Test
    void getSalesForDistributor_returnsEmptyList_whenNoSales() {
        List<Sale> sales = saleQueryApi.getSalesForDistributor(directDistributorId);

        assertThat(sales).isEmpty();
    }

    @Test
    void getSalesForDistributor_returnsSalesOrderedByDateDescending() {
        registerDirectSaleOnDate(5, LocalDate.of(2026, 1, 10));
        registerDirectSaleOnDate(3, LocalDate.of(2026, 3, 5));
        registerDirectSaleOnDate(7, LocalDate.of(2026, 2, 20));

        List<Sale> sales = saleQueryApi.getSalesForDistributor(directDistributorId);

        assertThat(sales).hasSize(3);
        assertThat(sales.get(0).saleDate()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(sales.get(1).saleDate()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(sales.get(2).saleDate()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    // ── getSalesForProductionRun ─────────────────────────────────────────────

    @Test
    void getSalesForProductionRun_returnsSalesContainingThatProductionRun() {
        registerDirectSale(5);
        registerDistributorSale(externalDistributorId, 10);

        List<Sale> sales = saleQueryApi.getSalesForProductionRun(productionRunId);

        assertThat(sales).hasSize(2);
    }

    @Test
    void getSalesForProductionRun_returnsEmptyList_whenNoSales() {
        List<Sale> sales = saleQueryApi.getSalesForProductionRun(productionRunId);

        assertThat(sales).isEmpty();
    }

    @Test
    void getSalesForProductionRun_excludesSalesForOtherProductionRuns() {
        // Create a second release + production run on a different label
        var otherLabel = labelTestHelper.createLabelWithDirectDistributor("Other Label");
        var otherReleaseId = releaseTestHelper.createReleaseEntity("Other Release", otherLabel.id());
        var otherDirectDistributorId = distributorRepository
                .findByLabelIdAndChannelType(otherLabel.id(), ChannelType.DIRECT)
                .orElseThrow()
                .getId();
        var otherProductionRun = productionRunRepository.save(new ProductionRunEntity(
                otherReleaseId, ReleaseFormat.VINYL, "Other pressing", "Plant B",
                LocalDate.of(2025, 1, 1), 50
        ));
        allocationCommandApi.createAllocation(otherProductionRun.getId(), otherDirectDistributorId, 50);

        // Sale for our production run
        registerDirectSale(5);

        // Sale for the OTHER production run
        saleCommandApi.registerSale(
                otherLabel.id(), LocalDate.of(2026, 2, 1), ChannelType.DIRECT,
                null, null,
                List.of(new SaleLineItemInput(otherReleaseId, ReleaseFormat.VINYL, 3,
                        Money.of(new BigDecimal("10.00"))))
        );

        List<Sale> sales = saleQueryApi.getSalesForProductionRun(productionRunId);

        assertThat(sales).hasSize(1);
        assertThat(sales.getFirst().labelId()).isEqualTo(labelId);
    }

    @Test
    void getSalesForProductionRun_separatesRepressingsWithSameReleaseAndFormat() {
        // Sale against the first pressing (findMostRecent picks productionRunId)
        registerDirectSale(5);

        // Create a repress of the same release+format with a later date
        var repress = productionRunRepository.save(new ProductionRunEntity(
                releaseId, ReleaseFormat.VINYL, "Second pressing", "Plant A",
                LocalDate.of(2026, 6, 1), 100
        ));
        allocationCommandApi.createAllocation(repress.getId(), directDistributorId, 100);

        // Sale against the repress (findMostRecent now returns the repress)
        registerDirectSale(3);

        List<Sale> salesForFirstPressing = saleQueryApi.getSalesForProductionRun(productionRunId);
        List<Sale> salesForRepress = saleQueryApi.getSalesForProductionRun(repress.getId());

        assertThat(salesForFirstPressing).hasSize(1);
        assertThat(salesForRepress).hasSize(1);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Sale registerDirectSale(int quantity) {
        return registerDirectSaleOnDate(quantity, LocalDate.of(2026, 2, 12));
    }

    private Sale registerDirectSaleOnDate(int quantity, LocalDate date) {
        return saleCommandApi.registerSale(
                labelId, date, ChannelType.DIRECT, null, null,
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, quantity,
                        Money.of(new BigDecimal("15.00"))))
        );
    }

    private Sale registerDistributorSale(Long distributorId, int quantity) {
        return saleCommandApi.registerSale(
                labelId, LocalDate.of(2026, 2, 12), ChannelType.DISTRIBUTOR, null, distributorId,
                List.of(new SaleLineItemInput(
                        releaseId, ReleaseFormat.VINYL, quantity,
                        Money.of(new BigDecimal("12.00"))))
        );
    }
}
