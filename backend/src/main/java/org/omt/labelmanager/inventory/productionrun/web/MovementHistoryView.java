package org.omt.labelmanager.inventory.productionrun.web;

import java.time.Instant;
import org.omt.labelmanager.inventory.LocationType;

/**
 * A single inventory movement.
 *
 * <p>Locations carry a type and, for {@code DISTRIBUTOR}, the distributor id — not a name. Naming
 * the distributor would make inventory read from distribution for presentation alone; the caller
 * joins against {@code /api/labels/{labelId}/distributors}, which it already has.
 */
public record MovementHistoryView(
        Instant occurredAt,
        org.omt.labelmanager.inventory.MovementType movementType,
        Location fromLocation,
        Location toLocation,
        int quantity) {

    /**
     * Where stock moved from or to. {@code distributorId} is null unless the type is DISTRIBUTOR.
     */
    public record Location(LocationType type, Long distributorId) {}
}
