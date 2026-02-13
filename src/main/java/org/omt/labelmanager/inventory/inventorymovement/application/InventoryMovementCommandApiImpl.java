package org.omt.labelmanager.inventory.inventorymovement.application;

import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.springframework.stereotype.Service;

@Service
class InventoryMovementCommandApiImpl implements InventoryMovementCommandApi {

    private final RecordMovementUseCase recordMovement;

    InventoryMovementCommandApiImpl(RecordMovementUseCase recordMovement) {
        this.recordMovement = recordMovement;
    }

    @Override
    public void recordMovement(
            Long productionRunId,
            Long distributorId,
            int quantityDelta,
            MovementType movementType,
            Long referenceId
    ) {
        recordMovement.execute(productionRunId, distributorId, quantityDelta, movementType, referenceId);
    }
}
