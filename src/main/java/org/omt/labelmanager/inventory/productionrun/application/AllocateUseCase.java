package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.domain.InventoryLocation;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
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
            InventoryMovementCommandApi inventoryMovementCommandApi
    ) {
        this.repository = repository;
        this.inventoryMovementQueryApi = inventoryMovementQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public void execute(Long productionRunId, InventoryLocation toLocation, int quantity) {
        ProductionRun run = repository.findById(productionRunId)
                .map(ProductionRun::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Production run not found: " + productionRunId
                ));

        int warehouseDelta = inventoryMovementQueryApi.getWarehouseInventory(productionRunId);
        int available = run.quantity() + warehouseDelta;

        if (quantity > available) {
            log.warn("Allocation rejected: requested {} but only {} available for run {}",
                    quantity, available, productionRunId);
            throw new InsufficientInventoryException(quantity, available);
        }

        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                InventoryLocation.warehouse(),
                toLocation,
                quantity,
                MovementType.ALLOCATION,
                null
        );
    }
}
