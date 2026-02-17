package org.omt.labelmanager.inventory.inventorymovement.domain;

import java.time.Instant;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementEntity;

/**
 * Represents a single inventory transfer between two locations.
 *
 * <p>All quantities are positive. Direction is expressed by {@code fromLocationType} and
 * {@code toLocationType}. Standard patterns:
 * <ul>
 *   <li>Allocation: {@code WAREHOUSE → DISTRIBUTOR(toLocationId)}</li>
 *   <li>Sale:       {@code DISTRIBUTOR(fromLocationId) → EXTERNAL}</li>
 *   <li>Return:     {@code DISTRIBUTOR(fromLocationId) → WAREHOUSE}</li>
 * </ul>
 */
public record InventoryMovement(
        Long id,
        Long productionRunId,
        LocationType fromLocationType,
        Long fromLocationId,
        LocationType toLocationType,
        Long toLocationId,
        int quantity,
        MovementType movementType,
        Instant occurredAt,
        Long referenceId
) {

    public static InventoryMovement fromEntity(InventoryMovementEntity entity) {
        return new InventoryMovement(
                entity.getId(),
                entity.getProductionRunId(),
                entity.getFromLocationType(),
                entity.getFromLocationId(),
                entity.getToLocationType(),
                entity.getToLocationId(),
                entity.getQuantity(),
                entity.getMovementType(),
                entity.getOccurredAt(),
                entity.getReferenceId()
        );
    }
}
