package org.omt.labelmanager.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MovementTypeTest {

    @Test
    void shouldHaveAllExpectedMovementTypes() {
        assertThat(MovementType.values())
                .containsExactlyInAnyOrder(
                        MovementType.ALLOCATION,
                        MovementType.SALE,
                        MovementType.TRANSFER_OUT,
                        MovementType.TRANSFER_IN,
                        MovementType.RETURN,
                        MovementType.ADJUSTMENT);
    }
}
