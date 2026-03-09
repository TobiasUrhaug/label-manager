package org.omt.labelmanager.inventory.inventorymovement.domain;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.omt.labelmanager.inventory.inventorymovement.domain.InventoryMovementFactory.anInventoryMovement;

class InventoryMovementTest {

    @Test
    void shouldCreateAllocationMovementWithCorrectFields() {
        Instant occurredAt = Instant.parse("2025-06-15T14:30:00Z");

        InventoryMovement movement = anInventoryMovement()
                .id(42L)
                .productionRunId(10L)
                .fromLocationType(LocationType.WAREHOUSE)
                .fromLocationId(null)
                .toLocationType(LocationType.DISTRIBUTOR)
                .toLocationId(5L)
                .quantity(200)
                .movementType(MovementType.ALLOCATION)
                .occurredAt(occurredAt)
                .referenceId(99L)
                .build();

        assertThat(movement.id()).isEqualTo(42L);
        assertThat(movement.productionRunId()).isEqualTo(10L);
        assertThat(movement.fromLocationType()).isEqualTo(LocationType.WAREHOUSE);
        assertThat(movement.fromLocationId()).isNull();
        assertThat(movement.toLocationType()).isEqualTo(LocationType.DISTRIBUTOR);
        assertThat(movement.toLocationId()).isEqualTo(5L);
        assertThat(movement.quantity()).isEqualTo(200);
        assertThat(movement.movementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(movement.occurredAt()).isEqualTo(occurredAt);
        assertThat(movement.referenceId()).isEqualTo(99L);
    }

    @Test
    void shouldCreateSaleMovementWithDistributorAsSource() {
        InventoryMovement movement = anInventoryMovement()
                .fromLocationType(LocationType.DISTRIBUTOR)
                .fromLocationId(7L)
                .toLocationType(LocationType.EXTERNAL)
                .toLocationId(null)
                .quantity(50)
                .movementType(MovementType.SALE)
                .referenceId(null)
                .build();

        assertThat(movement.fromLocationType()).isEqualTo(LocationType.DISTRIBUTOR);
        assertThat(movement.fromLocationId()).isEqualTo(7L);
        assertThat(movement.toLocationType()).isEqualTo(LocationType.EXTERNAL);
        assertThat(movement.toLocationId()).isNull();
        assertThat(movement.quantity()).isEqualTo(50);
        assertThat(movement.referenceId()).isNull();
    }

    @Test
    void shouldAllowNullReferenceId() {
        InventoryMovement movement = anInventoryMovement()
                .referenceId(null)
                .build();

        assertThat(movement.referenceId()).isNull();
    }
}
