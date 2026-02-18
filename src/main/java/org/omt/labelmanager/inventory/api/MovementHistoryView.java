package org.omt.labelmanager.inventory.api;

import java.time.Instant;
import org.omt.labelmanager.inventory.domain.MovementType;

/**
 * A single inventory movement for display in the movement history table.
 *
 * <p>Location names ({@code fromLocation}, {@code toLocation}) are pre-resolved
 * to human-readable strings (e.g. "Warehouse", "Direct Sales", "External (sold)")
 * so that templates do not need to perform distributor lookups.
 */
public record MovementHistoryView(
        Instant occurredAt,
        MovementType movementType,
        String fromLocation,
        String toLocation,
        int quantity
) {
}
