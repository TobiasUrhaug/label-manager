package org.omt.labelmanager.inventory.inventorymovement.domain;

import java.time.Instant;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementEntity;

public record InventoryMovement(
        Long id,
        Long productionRunId,
        Long distributorId,
        int quantityDelta,
        MovementType movementType,
        Instant occurredAt,
        Long referenceId
) {

    public static InventoryMovement fromEntity(InventoryMovementEntity entity) {
        return new InventoryMovement(
                entity.getId(),
                entity.getProductionRunId(),
                entity.getDistributorId(),
                entity.getQuantityDelta(),
                entity.getMovementType(),
                entity.getOccurredAt(),
                entity.getReferenceId()
        );
    }
}
