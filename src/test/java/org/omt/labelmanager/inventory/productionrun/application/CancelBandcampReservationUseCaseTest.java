package org.omt.labelmanager.inventory.productionrun.application;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.domain.InventoryLocation;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;

@ExtendWith(MockitoExtension.class)
class CancelBandcampReservationUseCaseTest {

    @Mock
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Mock
    private InventoryMovementCommandApi inventoryMovementCommandApi;

    private CancelBandcampReservationUseCase subject;

    private static final long RUN_ID = 1L;

    @BeforeEach
    void setUp() {
        subject = new CancelBandcampReservationUseCase(inventoryMovementQueryApi, inventoryMovementCommandApi);
    }

    @Test
    void execute_recordsReturnMovement_whenQuantityIsWithinHeld() {
        when(inventoryMovementQueryApi.getBandcampInventory(RUN_ID)).thenReturn(40);

        assertThatNoException().isThrownBy(() -> subject.execute(RUN_ID, 20));

        verify(inventoryMovementCommandApi).recordMovement(
                RUN_ID,
                InventoryLocation.bandcamp(),
                InventoryLocation.warehouse(),
                20,
                MovementType.RETURN,
                null
        );
    }

    @Test
    void execute_recordsReturnMovement_whenQuantityEqualsHeld() {
        when(inventoryMovementQueryApi.getBandcampInventory(RUN_ID)).thenReturn(40);

        assertThatNoException().isThrownBy(() -> subject.execute(RUN_ID, 40));

        verify(inventoryMovementCommandApi).recordMovement(
                RUN_ID,
                InventoryLocation.bandcamp(),
                InventoryLocation.warehouse(),
                40,
                MovementType.RETURN,
                null
        );
    }

    @Test
    void execute_throwsInsufficientInventoryException_whenQuantityExceedsHeld() {
        when(inventoryMovementQueryApi.getBandcampInventory(RUN_ID)).thenReturn(40);

        assertThatThrownBy(() -> subject.execute(RUN_ID, 50))
                .isInstanceOf(InsufficientInventoryException.class);
    }
}
