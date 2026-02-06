package org.omt.labelmanager.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MovementTypeTest {

    @Test
    void shouldHaveAllExpectedMovementTypes() {
        assertThat(MovementType.values()).containsExactlyInAnyOrder(
                MovementType.ALLOCATION,
                MovementType.SALE,
                MovementType.TRANSFER_OUT,
                MovementType.TRANSFER_IN,
                MovementType.RETURN,
                MovementType.ADJUSTMENT
        );
    }
}
