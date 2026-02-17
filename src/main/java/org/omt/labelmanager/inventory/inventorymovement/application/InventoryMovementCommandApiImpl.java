package org.omt.labelmanager.inventory.inventorymovement.application;

import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.springframework.stereotype.Service;

@Service
class InventoryMovementCommandApiImpl implements InventoryMovementCommandApi {

    private final RecordMovementUseCase recordMovement;
    private final DeleteMovementsUseCase deleteMovements;

    InventoryMovementCommandApiImpl(
            RecordMovementUseCase recordMovement,
            DeleteMovementsUseCase deleteMovements
    ) {
        this.recordMovement = recordMovement;
        this.deleteMovements = deleteMovements;
    }

    @Override
    public void recordMovement(
            Long productionRunId,
            LocationType fromLocationType,
            Long fromLocationId,
            LocationType toLocationType,
            Long toLocationId,
            int quantity,
            MovementType movementType,
            Long referenceId
    ) {
        recordMovement.execute(
                productionRunId,
                fromLocationType,
                fromLocationId,
                toLocationType,
                toLocationId,
                quantity,
                movementType,
                referenceId
        );
    }

    @Override
    public void deleteMovementsByReference(MovementType movementType, Long referenceId) {
        deleteMovements.execute(movementType, referenceId);
    }
}
