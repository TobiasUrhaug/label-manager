package org.omt.labelmanager.sales.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaleRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SaleCommandApi saleCommandApi;

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

    private Long labelId;
    private Long releaseId;
    private Long productionRunId;
    private Long directDistributorId;

    @BeforeEach
    void setUp() {
        // Clean up
        inventoryMovementRepository.deleteAll();
        channelAllocationRepository.deleteAll();
        productionRunRepository.deleteAll();

        // Create label (which auto-creates DIRECT distributor)
        var label = labelTestHelper.createLabel("Test Label");
        labelId = label.id();

        // Find the DIRECT distributor
        var directDistributor = distributorRepository
                .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                .orElseThrow();
        directDistributorId = directDistributor.getId();

        // Create release
        releaseId = releaseTestHelper.createReleaseEntity("Test Release", labelId);

        // Create production run
        var productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId,
                        ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        100
                ));
        productionRunId = productionRun.getId();

        // Allocate inventory to DIRECT distributor
        channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        directDistributorId,
                        50,
                        Instant.now()
                ));
    }

    @Test
    void registerSale_createsSaleWithLineItems() {
        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        5,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        var sale = saleCommandApi.registerSale(
                labelId,
                LocalDate.of(2026, 2, 12),
                ChannelType.EVENT,
                "Concert at venue X",
                lineItems
        );

        assertThat(sale.id()).isNotNull();
        assertThat(sale.labelId()).isEqualTo(labelId);
        assertThat(sale.saleDate()).isEqualTo(LocalDate.of(2026, 2, 12));
        assertThat(sale.channel()).isEqualTo(ChannelType.EVENT);
        assertThat(sale.notes()).isEqualTo("Concert at venue X");
        assertThat(sale.lineItems()).hasSize(1);
        assertThat(sale.totalAmount().amount())
                .isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void registerSale_reducesInventoryAllocation() {
        var initialAllocation = channelAllocationRepository
                .findByProductionRunIdAndDistributorId(
                        productionRunId,
                        directDistributorId
                )
                .orElseThrow();
        var initialQuantity = initialAllocation.getQuantity();

        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        10,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        saleCommandApi.registerSale(
                labelId,
                LocalDate.of(2026, 2, 12),
                ChannelType.EVENT,
                null,
                lineItems
        );

        var updatedAllocation = channelAllocationRepository
                .findByProductionRunIdAndDistributorId(
                        productionRunId,
                        directDistributorId
                )
                .orElseThrow();

        assertThat(updatedAllocation.getQuantity())
                .isEqualTo(initialQuantity - 10);
    }

    @Test
    void registerSale_createsInventoryMovement() {
        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        5,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        saleCommandApi.registerSale(
                labelId,
                LocalDate.of(2026, 2, 12),
                ChannelType.EVENT,
                null,
                lineItems
        );

        var movements = inventoryMovementRepository.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(1);
        assertThat(movements.getFirst().getMovementType()).isEqualTo(MovementType.SALE);
        assertThat(movements.getFirst().getQuantityDelta()).isEqualTo(-5);
        assertThat(movements.getFirst().getDistributorId()).isEqualTo(directDistributorId);
    }

    @Test
    void registerSale_withInsufficientInventory_throwsException() {
        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        100,  // More than available (50)
                        Money.of(new BigDecimal("15.00"))
                )
        );

        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.EVENT,
                        null,
                        lineItems
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient inventory");
    }

    @Test
    void registerSale_withNonExistentRelease_throwsException() {
        var lineItems = List.of(
                new SaleLineItemInput(
                        99999L,  // Non-existent release
                        ReleaseFormat.VINYL,
                        5,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.EVENT,
                        null,
                        lineItems
                ))
                .hasMessageContaining("Release not found");
    }

    @Test
    void registerSale_withReleaseFromDifferentLabel_throwsException() {
        var otherLabel = labelTestHelper.createLabel("Other Label");
        var otherReleaseId = releaseTestHelper.createReleaseEntity(
                "Other Release",
                otherLabel.id()
        );

        var lineItems = List.of(
                new SaleLineItemInput(
                        otherReleaseId,
                        ReleaseFormat.VINYL,
                        5,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.EVENT,
                        null,
                        lineItems
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to label");
    }
}
