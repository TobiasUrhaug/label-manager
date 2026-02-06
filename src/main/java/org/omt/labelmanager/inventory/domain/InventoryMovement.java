package org.omt.labelmanager.inventory.domain;

import java.time.Instant;

public record InventoryMovement(
        Long id,
        Long productionRunId,
        Long salesChannelId,
        int quantityDelta,
        MovementType movementType,
        Instant occurredAt,
        Long referenceId
) {
}
