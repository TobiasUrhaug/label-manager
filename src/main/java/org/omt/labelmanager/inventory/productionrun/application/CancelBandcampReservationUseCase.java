package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.domain.InventoryLocation;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CancelBandcampReservationUseCase {

    private final InventoryMovementQueryApi inventoryMovementQueryApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    CancelBandcampReservationUseCase(
            InventoryMovementQueryApi inventoryMovementQueryApi,
            InventoryMovementCommandApi inventoryMovementCommandApi
    ) {
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public void execute(Long productionRunId, int quantity) {
        int held = inventoryMovementQueryApi.getBandcampInventory(productionRunId);

        if (quantity > held) {
            throw new InsufficientInventoryException(quantity, held);
        }

        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                InventoryLocation.bandcamp(),
                InventoryLocation.warehouse(),
                quantity,
                MovementType.RETURN,
                null
        );
    }
}
