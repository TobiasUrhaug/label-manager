package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AllocateUseCase {

    private static final Logger log = LoggerFactory.getLogger(AllocateUseCase.class);

    private final ProductionRunRepository repository;
    private final InventoryMovementQueryApi inventoryMovementQueryApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    AllocateUseCase(
            ProductionRunRepository repository,
            InventoryMovementQueryApi inventoryMovementQueryApi,
            InventoryMovementCommandApi inventoryMovementCommandApi) {
        this.repository = repository;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public void execute(Long productionRunId, InventoryLocation toLocation, int quantity) {
        // Locked, not merely fetched: reading the warehouse balance and inserting the movement that
        // consumes it are two statements, so two concurrent allocations of the last units would
        // both see them free. Same mutex the sale path takes.
        if (repository.lockById(productionRunId).isEmpty()) {
            throw new IllegalArgumentException("Production run not found: " + productionRunId);
        }

        // Absolute, not a delta: manufacture is a PRODUCTION movement, so the ledger already
        // includes the run's quantity (V33).
        int available = inventoryMovementQueryApi.getWarehouseInventory(productionRunId);

        if (quantity > available) {
            log.warn(
                    "Allocation rejected: requested {} but only {} available for run {}",
                    quantity,
                    available,
                    productionRunId);
            throw new InsufficientInventoryException(quantity, available);
        }

        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                InventoryLocation.warehouse(),
                toLocation,
                quantity,
                MovementType.ALLOCATION,
                null);
    }
}
