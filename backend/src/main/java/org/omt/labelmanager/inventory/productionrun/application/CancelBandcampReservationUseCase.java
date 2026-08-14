package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CancelBandcampReservationUseCase {

    private final ProductionRunRepository repository;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    CancelBandcampReservationUseCase(
            ProductionRunRepository repository,
            InventoryMovementQueryApi inventoryMovementQueryApi,
            InventoryMovementCommandApi inventoryMovementCommandApi) {
        this.repository = repository;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public void execute(Long productionRunId, int quantity) {
        // Locked before reading, for the same reason as allocation: the check and the movement that
        // consumes what it checked are two statements.
        repository.lockById(productionRunId);

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
                null);
    }
}
