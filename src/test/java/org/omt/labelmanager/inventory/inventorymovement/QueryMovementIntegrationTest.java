package org.omt.labelmanager.inventory.inventorymovement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.omt.labelmanager.inventory.domain.InventoryLocation.distributor;
import static org.omt.labelmanager.inventory.domain.InventoryLocation.external;
import static org.omt.labelmanager.inventory.domain.InventoryLocation.warehouse;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorRepository;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class QueryMovementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryMovementCommandApi inventoryMovementCommandApi;

    @Autowired
    private InventoryMovementQueryApi inventoryMovementQueryApi;

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
        productionRunRepository.deleteAll();
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper
                .createReleaseEntity("Test Release", label.id());

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId, ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A", LocalDate.of(2025, 1, 1), 500));
        productionRunId = productionRun.getId();

        DistributorEntity distributor = distributorRepository.save(
                new DistributorEntity(
                        label.id(), "Direct Sales",
                        ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    @Test
    void findByProductionRunId_returnsAllMovements() {
        recordAllocation(100);
        recordSale(20);

        var movements = inventoryMovementQueryApi
                .findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(
                m -> m.productionRunId().equals(productionRunId));
    }

    @Test
    void findByProductionRunId_returnsEmptyListWhenNoMovements() {
        var movements = inventoryMovementQueryApi
                .findByProductionRunId(productionRunId);

        assertThat(movements).isEmpty();
    }

    @Test
    void getCurrentInventory_returnsInboundMinusOutbound() {
        recordAllocation(200);
        recordSale(50);

        int current = inventoryMovementQueryApi
                .getCurrentInventory(productionRunId, distributorId);

        assertThat(current).isEqualTo(150);
    }

    @Test
    void getCurrentInventory_returnsZeroWhenNothingAllocated() {
        int current = inventoryMovementQueryApi
                .getCurrentInventory(productionRunId, distributorId);

        assertThat(current).isEqualTo(0);
    }

    @Test
    void getWarehouseInventory_returnsWarehouseBalance() {
        recordAllocation(200);

        int warehouse = inventoryMovementQueryApi
                .getWarehouseInventory(productionRunId);

        // 0 inbound - 200 outbound = -200
        assertThat(warehouse).isEqualTo(-200);
    }

    @Test
    void getCurrentInventoryByDistributor_returnsCurrentQuantities() {
        recordAllocation(300);
        recordSale(100);

        var byDistributor = inventoryMovementQueryApi
                .getCurrentInventoryByDistributor(productionRunId);

        assertThat(byDistributor).containsEntry(distributorId, 200);
    }

    @Test
    void getCurrentInventoryByDistributor_excludesZeroInventory() {
        recordAllocation(100);
        recordSale(100);

        var byDistributor = inventoryMovementQueryApi
                .getCurrentInventoryByDistributor(productionRunId);

        assertThat(byDistributor).doesNotContainKey(distributorId);
    }

    @Test
    void getMovementsForProductionRun_returnsNewestFirst() {
        recordAllocation(100);
        recordSale(20);

        var movements = inventoryMovementQueryApi
                .getMovementsForProductionRun(productionRunId);

        assertThat(movements).hasSize(2);
        assertThat(movements.get(0).movementType())
                .isEqualTo(MovementType.SALE);
        assertThat(movements.get(1).movementType())
                .isEqualTo(MovementType.ALLOCATION);
    }

    private void recordAllocation(int quantity) {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                warehouse(), distributor(distributorId),
                quantity, MovementType.ALLOCATION, null);
    }

    private void recordSale(int quantity) {
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                distributor(distributorId), external(),
                quantity, MovementType.SALE, null);
    }
}
