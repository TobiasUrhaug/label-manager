package org.omt.labelmanager.inventory.inventorymovement;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

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
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId,
                        ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        500
                ));
        productionRunId = productionRun.getId();

        DistributorEntity distributor = distributorRepository.save(
                new DistributorEntity(label.id(), "Direct Sales", ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    @Test
    void findByProductionRunId_returnsAllMovementsForProductionRun() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId, distributorId, 100, MovementType.ALLOCATION, null);
        inventoryMovementCommandApi.recordMovement(
                productionRunId, distributorId, -20, MovementType.SALE, null);

        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.productionRunId().equals(productionRunId));
    }

    @Test
    void findByDistributorId_returnsAllMovementsForDistributor() {
        inventoryMovementCommandApi.recordMovement(
                productionRunId, distributorId, 100, MovementType.ALLOCATION, null);
        inventoryMovementCommandApi.recordMovement(
                productionRunId, distributorId, -20, MovementType.SALE, null);

        var movements = inventoryMovementQueryApi.findByDistributorId(distributorId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.distributorId().equals(distributorId));
    }

    @Test
    void findByProductionRunId_returnsEmptyListWhenNoMovements() {
        var movements = inventoryMovementQueryApi.findByProductionRunId(productionRunId);

        assertThat(movements).isEmpty();
    }

    @Test
    void findByDistributorId_returnsEmptyListWhenNoMovements() {
        var movements = inventoryMovementQueryApi.findByDistributorId(distributorId);

        assertThat(movements).isEmpty();
    }
}
