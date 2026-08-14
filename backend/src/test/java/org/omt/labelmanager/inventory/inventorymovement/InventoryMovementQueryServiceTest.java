package org.omt.labelmanager.inventory.inventorymovement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.persistence.DistributorRepository;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.LocationType;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.api.LocationBalance;
import org.omt.labelmanager.inventory.inventorymovement.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.inventorymovement.persistence.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;
import org.omt.labelmanager.shared.Format;
import org.springframework.beans.factory.annotation.Autowired;

public class InventoryMovementQueryServiceTest extends AbstractIntegrationTest {

    @Autowired private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Autowired private InventoryMovementRepository movementRepository;

    @Autowired private ProductionRunRepository productionRunRepository;

    @Autowired private DistributorRepository distributorRepository;

    @Autowired private LabelTestHelper labelTestHelper;

    @Autowired private ReleaseTestHelper releaseTestHelper;

    private Long productionRunId;
    private Long distributorId;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        productionRunRepository.deleteAll();
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        ProductionRunEntity run =
                productionRunRepository.save(
                        new ProductionRunEntity(
                                releaseId,
                                Format.VINYL,
                                "First pressing",
                                "Plant A",
                                LocalDate.of(2025, 1, 1),
                                500));
        productionRunId = run.getId();

        DistributorEntity distributor =
                distributorRepository.save(
                        new DistributorEntity(label.id(), "Test Distro", ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    // --- getBandcampInventory ---

    @Test
    void getBandcampInventory_returnsNetHeld_afterAllocationAndSale() {
        saveMovement(
                productionRunId,
                LocationType.WAREHOUSE,
                null,
                LocationType.BANDCAMP,
                null,
                50,
                MovementType.ALLOCATION);
        saveMovement(
                productionRunId,
                LocationType.BANDCAMP,
                null,
                LocationType.EXTERNAL,
                null,
                10,
                MovementType.SALE);

        assertThat(inventoryMovementQueryApi.getBandcampInventory(productionRunId)).isEqualTo(40);
    }

    @Test
    void getBandcampInventory_returnsNetHeld_afterAllocationSaleAndReturn() {
        saveMovement(
                productionRunId,
                LocationType.WAREHOUSE,
                null,
                LocationType.BANDCAMP,
                null,
                50,
                MovementType.ALLOCATION);
        saveMovement(
                productionRunId,
                LocationType.BANDCAMP,
                null,
                LocationType.EXTERNAL,
                null,
                10,
                MovementType.SALE);
        saveMovement(
                productionRunId,
                LocationType.BANDCAMP,
                null,
                LocationType.WAREHOUSE,
                null,
                10,
                MovementType.RETURN);

        assertThat(inventoryMovementQueryApi.getBandcampInventory(productionRunId)).isEqualTo(30);
    }

    // --- getProductionRunIdsAllocatedToDistributor ---

    @Test
    void
            getProductionRunIdsAllocatedToDistributor_returnsBothRunIds_whenEachHasAllocationToDistributor() {
        var label = labelTestHelper.createLabel("Label 2");
        Long releaseId2 = releaseTestHelper.createReleaseEntity("Release 2", label.id());
        Long runId2 =
                productionRunRepository
                        .save(
                                new ProductionRunEntity(
                                        releaseId2,
                                        Format.VINYL,
                                        "Second pressing",
                                        "Plant B",
                                        LocalDate.of(2025, 6, 1),
                                        300))
                        .getId();

        saveMovement(
                productionRunId,
                LocationType.WAREHOUSE,
                null,
                LocationType.DISTRIBUTOR,
                distributorId,
                100,
                MovementType.ALLOCATION);
        saveMovement(
                runId2,
                LocationType.WAREHOUSE,
                null,
                LocationType.DISTRIBUTOR,
                distributorId,
                50,
                MovementType.ALLOCATION);

        var result =
                inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(distributorId);

        assertThat(result).containsExactlyInAnyOrder(productionRunId, runId2);
    }

    @Test
    void getProductionRunIdsAllocatedToDistributor_excludesRun_whenOnlySaleWithoutAllocation() {
        saveMovement(
                productionRunId,
                LocationType.DISTRIBUTOR,
                distributorId,
                LocationType.EXTERNAL,
                null,
                20,
                MovementType.SALE);

        var result =
                inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(distributorId);

        assertThat(result).isEmpty();
    }

    // --- balancesFor: the aggregate every other balance question is answered from ---

    @Test
    void balancesFor_reportsEveryLocationOfEveryRunInOneCall() {
        Long secondRun = anotherProductionRun();
        saveMovement(
                productionRunId,
                LocationType.EXTERNAL,
                null,
                LocationType.WAREHOUSE,
                null,
                500,
                MovementType.PRODUCTION);
        saveMovement(
                productionRunId,
                LocationType.WAREHOUSE,
                null,
                LocationType.DISTRIBUTOR,
                distributorId,
                120,
                MovementType.ALLOCATION);
        saveMovement(
                secondRun,
                LocationType.EXTERNAL,
                null,
                LocationType.WAREHOUSE,
                null,
                300,
                MovementType.PRODUCTION);

        var balances = inventoryMovementQueryApi.balancesFor(List.of(productionRunId, secondRun));

        assertThat(balances)
                .containsExactlyInAnyOrder(
                        new LocationBalance(productionRunId, InventoryLocation.warehouse(), 380),
                        new LocationBalance(
                                productionRunId, InventoryLocation.distributor(distributorId), 120),
                        new LocationBalance(productionRunId, InventoryLocation.external(), -500),
                        new LocationBalance(secondRun, InventoryLocation.warehouse(), 300),
                        new LocationBalance(secondRun, InventoryLocation.external(), -300));
    }

    @Test
    void balancesFor_omitsLocationsThatNetToZero() {
        saveMovement(
                productionRunId,
                LocationType.WAREHOUSE,
                null,
                LocationType.DISTRIBUTOR,
                distributorId,
                50,
                MovementType.ALLOCATION);
        saveMovement(
                productionRunId,
                LocationType.DISTRIBUTOR,
                distributorId,
                LocationType.WAREHOUSE,
                null,
                50,
                MovementType.RETURN);

        var balances = inventoryMovementQueryApi.balancesFor(List.of(productionRunId));

        assertThat(balances).isEmpty();
    }

    @Test
    void balancesFor_returnsNothingForNoRuns() {
        assertThat(inventoryMovementQueryApi.balancesFor(List.of())).isEmpty();
    }

    @Test
    void findByProductionRunIds_groupsMovementsByRunNewestFirst() {
        Long secondRun = anotherProductionRun();
        saveMovementAt(
                productionRunId,
                LocationType.EXTERNAL,
                null,
                LocationType.WAREHOUSE,
                null,
                500,
                MovementType.PRODUCTION,
                Instant.parse("2025-01-01T00:00:00Z"));
        saveMovementAt(
                productionRunId,
                LocationType.WAREHOUSE,
                null,
                LocationType.DISTRIBUTOR,
                distributorId,
                120,
                MovementType.ALLOCATION,
                Instant.parse("2025-03-01T00:00:00Z"));
        saveMovementAt(
                secondRun,
                LocationType.EXTERNAL,
                null,
                LocationType.WAREHOUSE,
                null,
                300,
                MovementType.PRODUCTION,
                Instant.parse("2025-02-01T00:00:00Z"));

        var byRun =
                inventoryMovementQueryApi.findByProductionRunIds(
                        List.of(productionRunId, secondRun));

        assertThat(byRun).hasSize(2);
        assertThat(byRun.get(productionRunId))
                .extracting(InventoryMovement::movementType)
                .containsExactly(MovementType.ALLOCATION, MovementType.PRODUCTION);
        assertThat(byRun.get(secondRun)).singleElement().returns(300, InventoryMovement::quantity);
    }

    private Long anotherProductionRun() {
        var label = labelTestHelper.createLabel("Other Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Other Release", label.id());
        return productionRunRepository
                .save(
                        new ProductionRunEntity(
                                releaseId,
                                Format.VINYL,
                                "Second pressing",
                                "Plant B",
                                LocalDate.of(2026, 1, 1),
                                300))
                .getId();
    }

    private void saveMovement(
            Long runId,
            LocationType fromType,
            Long fromId,
            LocationType toType,
            Long toId,
            int quantity,
            MovementType movementType) {
        saveMovementAt(
                runId, fromType, fromId, toType, toId, quantity, movementType, Instant.now());
    }

    private void saveMovementAt(
            Long runId,
            LocationType fromType,
            Long fromId,
            LocationType toType,
            Long toId,
            int quantity,
            MovementType movementType,
            Instant occurredAt) {
        movementRepository.save(
                new InventoryMovementEntity(
                        runId,
                        fromType,
                        fromId,
                        toType,
                        toId,
                        quantity,
                        movementType,
                        occurredAt,
                        null));
    }
}
