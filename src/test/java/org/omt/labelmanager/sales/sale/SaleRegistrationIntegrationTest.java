package org.omt.labelmanager.sales.sale;

import java.math.BigDecimal;
import java.time.Instant;
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
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Autowired
    private AllocationCommandApi allocationCommandApi;

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

        // Create label with DIRECT distributor
        var label = labelTestHelper.createLabelWithDirectDistributor("Test Label");
        labelId = label.id();

        // Get the DIRECT distributor
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

        // Allocate inventory to DIRECT distributor (records movement via API)
        allocationCommandApi.createAllocation(productionRunId, directDistributorId, 50);
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
                ChannelType.DIRECT,
                "Concert at venue X",
                null,
                lineItems
        );

        assertThat(sale.id()).isNotNull();
        assertThat(sale.labelId()).isEqualTo(labelId);
        assertThat(sale.saleDate()).isEqualTo(LocalDate.of(2026, 2, 12));
        assertThat(sale.channel()).isEqualTo(ChannelType.DIRECT);
        assertThat(sale.notes()).isEqualTo("Concert at venue X");
        assertThat(sale.lineItems()).hasSize(1);
        assertThat(sale.totalAmount().amount())
                .isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void registerSale_decreasesCurrentInventory() {
        int inventoryBefore = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, directDistributorId
        );

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
                ChannelType.DIRECT,
                null,
                null,
                lineItems
        );

        int inventoryAfter = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, directDistributorId
        );

        assertThat(inventoryAfter).isEqualTo(inventoryBefore - 10);
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

        var sale = saleCommandApi.registerSale(
                labelId,
                LocalDate.of(2026, 2, 12),
                ChannelType.DIRECT,
                null,
                null,
                lineItems
        );

        var movements = inventoryMovementRepository
                .findByProductionRunIdOrderByOccurredAtDesc(productionRunId)
                .stream()
                .filter(m -> m.getMovementType() == MovementType.SALE)
                .toList();

        assertThat(movements).hasSize(1);
        assertThat(movements.getFirst().getMovementType()).isEqualTo(MovementType.SALE);
        assertThat(movements.getFirst().getFromLocationType()).isEqualTo(
                org.omt.labelmanager.inventory.domain.LocationType.DISTRIBUTOR);
        assertThat(movements.getFirst().getFromLocationId()).isEqualTo(directDistributorId);
        assertThat(movements.getFirst().getToLocationType()).isEqualTo(
                org.omt.labelmanager.inventory.domain.LocationType.EXTERNAL);
        assertThat(movements.getFirst().getQuantity()).isEqualTo(5);
        assertThat(movements.getFirst().getReferenceId()).isEqualTo(sale.id());
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
                        ChannelType.DIRECT,
                        null,
                        null,
                        lineItems
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient quantity");
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
                        ChannelType.DIRECT,
                        null,
                        null,
                        lineItems
                ))
                .hasMessageContaining("Release not found");
    }

    @Test
    void registerSale_withReleaseFromDifferentLabel_throwsException() {
        var otherLabel = labelTestHelper.createLabelWithDirectDistributor("Other Label");
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
                        ChannelType.DIRECT,
                        null,
                        null,
                        lineItems
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to label");
    }

    @Test
    void registerSale_withNoAllocation_throwsHelpfulException() {
        // Create a second production run without any allocation
        productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId,
                        ReleaseFormat.CD,
                        "CD pressing",
                        "Plant B",
                        LocalDate.of(2025, 2, 1),
                        100
                ));

        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.CD,
                        5,
                        Money.of(new BigDecimal("12.00"))
                )
        );

        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.DIRECT,
                        null,
                        null,
                        lineItems
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No inventory allocated")
                .hasMessageContaining("Test Release")
                .hasMessageContaining("CD")
                .hasMessageContaining(
                        "allocate inventory from the production run"
                );
    }

    @Test
    void registerSale_withNoProductionRun_throwsHelpfulException() {
        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.CD,  // No production run for CD format
                        5,
                        Money.of(new BigDecimal("12.00"))
                )
        );

        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.DIRECT,
                        null,
                        null,
                        lineItems
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No production run found")
                .hasMessageContaining("Test Release")
                .hasMessageContaining("CD")
                .hasMessageContaining("create a production run");
    }

    @Test
    void registerDirectSale_autoSelectsDirectDistributor() {
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
                ChannelType.DIRECT,
                null,
                null,  // No distributorId needed for DIRECT sales
                lineItems
        );

        assertThat(sale.channel()).isEqualTo(ChannelType.DIRECT);

        // Verify DIRECT distributor inventory decreased
        int currentInventory = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, directDistributorId
        );
        assertThat(currentInventory).isEqualTo(45); // 50 allocated - 5 sold
    }

    @Test
    void registerDistributorSale_updatesCorrectDistributorAllocation() {
        // Create a DISTRIBUTOR type distributor
        var distributorEntity = distributorRepository.save(
                new DistributorEntity(
                        labelId,
                        "Big Cartel",
                        ChannelType.DISTRIBUTOR
                ));
        Long distributorId = distributorEntity.getId();

        // Allocate inventory to this distributor (via API so movements are recorded)
        allocationCommandApi.createAllocation(productionRunId, distributorId, 30);

        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        10,
                        Money.of(new BigDecimal("12.00"))
                )
        );

        var sale = saleCommandApi.registerSale(
                labelId,
                LocalDate.of(2026, 2, 12),
                ChannelType.DISTRIBUTOR,
                "Sale via Big Cartel",
                distributorId,
                lineItems
        );

        assertThat(sale.channel()).isEqualTo(ChannelType.DISTRIBUTOR);

        // Verify the DISTRIBUTOR's inventory decreased
        int distributorInventory = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, distributorId
        );
        assertThat(distributorInventory).isEqualTo(20); // 30 allocated - 10 sold

        // Verify DIRECT allocation was NOT touched
        int directInventory = inventoryMovementQueryApi.getCurrentInventory(
                productionRunId, directDistributorId
        );
        assertThat(directInventory).isEqualTo(50); // unchanged
    }

    @Test
    void registerSale_withMismatchedChannelType_throwsException() {
        // Create a RECORD_STORE distributor
        var recordStoreEntity = distributorRepository.save(
                new DistributorEntity(
                        labelId,
                        "Cool Records",
                        ChannelType.RECORD_STORE
                ));

        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        5,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        // Try to register DISTRIBUTOR sale but provide RECORD_STORE distributor
        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.DISTRIBUTOR,
                        null,
                        recordStoreEntity.getId(),
                        lineItems
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match channel type");
    }

    @Test
    void registerSale_withEmptyLineItems_throwsException() {
        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.DIRECT,
                        null,
                        null,
                        List.of()
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one line item");
    }

    @Test
    void registerNonDirectSale_withoutDistributorId_throwsException() {
        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        5,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.DISTRIBUTOR,
                        null,
                        null,  // Missing distributorId
                        lineItems
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distributor must be specified");
    }

    @Test
    void registerDistributorSale_withNoAllocation_throwsHelpfulException() {
        // Create distributor WITHOUT allocation
        var distributorEntity = distributorRepository.save(
                new DistributorEntity(
                        labelId,
                        "Unallocated Distributor",
                        ChannelType.DISTRIBUTOR
                ));

        var lineItems = List.of(
                new SaleLineItemInput(
                        releaseId,
                        ReleaseFormat.VINYL,
                        5,
                        Money.of(new BigDecimal("15.00"))
                )
        );

        assertThatThrownBy(() ->
                saleCommandApi.registerSale(
                        labelId,
                        LocalDate.of(2026, 2, 12),
                        ChannelType.DISTRIBUTOR,
                        null,
                        distributorEntity.getId(),
                        lineItems
                ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No inventory allocated")
                .hasMessageContaining("Unallocated Distributor");
    }
}
