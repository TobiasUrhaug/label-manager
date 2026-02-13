package org.omt.labelmanager.inventory.inventorymovement.api;

import org.omt.labelmanager.inventory.domain.MovementType;

/**
 * Public API for inventory movement command operations.
 */
public interface InventoryMovementCommandApi {

    /**
     * Record an inventory movement.
     *
     * @param productionRunId the production run
     * @param distributorId the distributor
     * @param quantityDelta the quantity change (negative for outbound, positive for inbound)
     * @param movementType the type of movement (SALE, ALLOCATION, etc.)
     * @param referenceId optional reference to the triggering entity (e.g., sale ID)
     */
    void recordMovement(
            Long productionRunId,
            Long distributorId,
            int quantityDelta,
            MovementType movementType,
            Long referenceId
    );
}
