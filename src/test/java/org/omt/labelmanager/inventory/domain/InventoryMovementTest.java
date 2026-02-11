package org.omt.labelmanager.inventory.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.omt.labelmanager.inventory.domain.InventoryMovementFactory.anInventoryMovement;

class InventoryMovementTest {

    @Test
    void shouldCreateInventoryMovementWithAllFields() {
        Instant occurredAt = Instant.parse("2025-06-15T14:30:00Z");

        InventoryMovement movement = anInventoryMovement()
                .id(42L)
                .productionRunId(10L)
                .distributorId(5L)
                .quantityDelta(200)
                .movementType(MovementType.ALLOCATION)
                .occurredAt(occurredAt)
                .referenceId(99L)
                .build();

        assertThat(movement.id()).isEqualTo(42L);
        assertThat(movement.productionRunId()).isEqualTo(10L);
        assertThat(movement.distributorId()).isEqualTo(5L);
        assertThat(movement.quantityDelta()).isEqualTo(200);
        assertThat(movement.movementType()).isEqualTo(MovementType.ALLOCATION);
        assertThat(movement.occurredAt()).isEqualTo(occurredAt);
        assertThat(movement.referenceId()).isEqualTo(99L);
    }

    @Test
    void shouldAllowNullReferenceId() {
        InventoryMovement movement = anInventoryMovement()
                .referenceId(null)
                .build();

        assertThat(movement.referenceId()).isNull();
    }
}
