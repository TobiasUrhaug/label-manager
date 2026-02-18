package org.omt.labelmanager.inventory.inventorymovement.infrastructure;

import java.time.Instant;
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
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class InventoryMovementPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

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
        inventoryMovementRepository.deleteAll();
        productionRunRepository.deleteAll();
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity("Test Release", label.id());

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId, ReleaseFormat.VINYL, "First pressing",
                        "Plant A", LocalDate.of(2025, 1, 1), 500));
        productionRunId = productionRun.getId();

        DistributorEntity distributor = distributorRepository.save(
                new DistributorEntity(label.id(), "Direct Sales", ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    @Test
    void savesAndRetrievesAllocationMovement() {
        Instant occurredAt = Instant.parse("2025-06-15T10:00:00Z");
        var entity = new InventoryMovementEntity(
                productionRunId,
                LocationType.WAREHOUSE, null,
                LocationType.DISTRIBUTOR, distributorId,
                100, MovementType.ALLOCATION, occurredAt, 42L
        );

        var saved = inventoryMovementRepository.save(entity);

        var retrieved = inventoryMovementRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        var m = retrieved.get();
        assertThat(m.getProductionRunId()).isEqualTo(productionRunId);
        assertThat(m.getFromLocationType()).isEqualTo(LocationType.WAREHOUSE);
        assertThat(m.getFromLocationId()).isNull();
        assertThat(m.getToLocationType()).isEqualTo(LocationType.DISTRIBUTOR);
        assertThat(m.getToLocationId()).isEqualTo(distributorId);
        assertThat(m.getQuantity()).isEqualTo(100);
        assertThat(m.getMovementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(m.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(m.getReferenceId()).isEqualTo(42L);
    }

    @Test
    void savesMovementWithNullReferenceId() {
        var entity = new InventoryMovementEntity(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId,
                LocationType.EXTERNAL, null,
                50, MovementType.SALE, Instant.now(), null
        );

        var saved = inventoryMovementRepository.save(entity);

        var retrieved = inventoryMovementRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getReferenceId()).isNull();
    }

    @Test
    void findsByProductionRunId() {
        Instant occurredAt = Instant.now();
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId,
                LocationType.WAREHOUSE, null, LocationType.DISTRIBUTOR, distributorId,
                100, MovementType.ALLOCATION, occurredAt, null));
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId, LocationType.EXTERNAL, null,
                20, MovementType.SALE, occurredAt, null));

        var movements =
                inventoryMovementRepository.findByProductionRunIdOrderByOccurredAtDesc(
                        productionRunId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.getProductionRunId().equals(productionRunId));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void deletesMovementsByReferenceId() {
        Instant occurredAt = Instant.now();
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId, LocationType.EXTERNAL, null,
                30, MovementType.SALE, occurredAt, 99L));
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId, LocationType.EXTERNAL, null,
                20, MovementType.SALE, occurredAt, 99L));
        // Different referenceId — should NOT be deleted
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId,
                LocationType.DISTRIBUTOR, distributorId, LocationType.EXTERNAL, null,
                10, MovementType.SALE, occurredAt, 100L));

        inventoryMovementRepository.deleteByMovementTypeAndReferenceId(MovementType.SALE, 99L);

        var remaining =
                inventoryMovementRepository.findByProductionRunIdOrderByOccurredAtDesc(
                        productionRunId);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.getFirst().getReferenceId()).isEqualTo(100L);
    }

    @Test
    void deletesMovementWhenProductionRunDeleted() {
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId,
                LocationType.WAREHOUSE, null, LocationType.DISTRIBUTOR, distributorId,
                100, MovementType.ALLOCATION, Instant.now(), null));

        assertThat(inventoryMovementRepository
                .findByProductionRunIdOrderByOccurredAtDesc(productionRunId)).hasSize(1);

        productionRunRepository.deleteById(productionRunId);

        assertThat(inventoryMovementRepository
                .findByProductionRunIdOrderByOccurredAtDesc(productionRunId)).isEmpty();
    }
}
