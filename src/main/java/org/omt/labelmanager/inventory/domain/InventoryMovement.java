package org.omt.labelmanager.inventory.domain;

import java.time.Instant;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementEntity;

public record InventoryMovement(
        Long id,
        Long productionRunId,
        Long salesChannelId,
        int quantityDelta,
        MovementType movementType,
        Instant occurredAt,
        Long referenceId
) {

    public static InventoryMovement fromEntity(InventoryMovementEntity entity) {
        return new InventoryMovement(
                entity.getId(),
                entity.getProductionRunId(),
                entity.getSalesChannelId(),
                entity.getQuantityDelta(),
                entity.getMovementType(),
                entity.getOccurredAt(),
                entity.getReferenceId()
        );
    }
}
