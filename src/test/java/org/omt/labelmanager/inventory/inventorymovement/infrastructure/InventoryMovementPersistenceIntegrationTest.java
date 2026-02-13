package org.omt.labelmanager.inventory.inventorymovement.infrastructure;

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
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;

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
    void savesAndRetrievesInventoryMovement() {
        Instant occurredAt = Instant.parse("2025-06-15T10:00:00Z");
        var entity = new InventoryMovementEntity(
                productionRunId,
                distributorId,
                100,
                MovementType.ALLOCATION,
                occurredAt,
                42L
        );

        var saved = inventoryMovementRepository.save(entity);

        var retrieved = inventoryMovementRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProductionRunId()).isEqualTo(productionRunId);
        assertThat(retrieved.get().getDistributorId()).isEqualTo(distributorId);
        assertThat(retrieved.get().getQuantityDelta()).isEqualTo(100);
        assertThat(retrieved.get().getMovementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(retrieved.get().getOccurredAt()).isEqualTo(occurredAt);
        assertThat(retrieved.get().getReferenceId()).isEqualTo(42L);
    }

    @Test
    void savesMovementWithNullReferenceId() {
        Instant occurredAt = Instant.now();
        var entity = new InventoryMovementEntity(
                productionRunId,
                distributorId,
                -50,
                MovementType.SALE,
                occurredAt,
                null
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
                productionRunId, distributorId, 100, MovementType.ALLOCATION, occurredAt, null));
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, distributorId, -20, MovementType.SALE, occurredAt, null));

        var movements = inventoryMovementRepository.findByProductionRunId(productionRunId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.getProductionRunId().equals(productionRunId));
    }

    @Test
    void findsByDistributorId() {
        Instant occurredAt = Instant.now();
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, distributorId, 100, MovementType.ALLOCATION, occurredAt, null));
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, distributorId, -20, MovementType.SALE, occurredAt, null));

        var movements = inventoryMovementRepository.findByDistributorId(distributorId);

        assertThat(movements).hasSize(2);
        assertThat(movements).allMatch(m -> m.getDistributorId().equals(distributorId));
    }

    @Test
    void deletesMovementWhenProductionRunDeleted() {
        Instant occurredAt = Instant.now();
        inventoryMovementRepository.save(new InventoryMovementEntity(
                productionRunId, distributorId, 100, MovementType.ALLOCATION, occurredAt, null));

        assertThat(inventoryMovementRepository.findByProductionRunId(productionRunId)).hasSize(1);

        productionRunRepository.deleteById(productionRunId);

        assertThat(inventoryMovementRepository.findByProductionRunId(productionRunId)).isEmpty();
    }
}
