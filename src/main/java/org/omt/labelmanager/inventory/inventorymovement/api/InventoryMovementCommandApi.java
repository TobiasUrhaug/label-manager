package org.omt.labelmanager.inventory.inventorymovement.api;

import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;

/**
 * Public API for inventory movement command operations.
 */
public interface InventoryMovementCommandApi {

    /**
     * Records a bidirectional inventory transfer.
     *
     * <p>Standard usage patterns:
     * <ul>
     *   <li>Allocation: {@code from=WAREHOUSE, fromId=null, to=DISTRIBUTOR, toId=distId}</li>
     *   <li>Sale:       {@code from=DISTRIBUTOR, fromId=distId, to=EXTERNAL, toId=null}</li>
     *   <li>Return:     {@code from=DISTRIBUTOR, fromId=distId, to=WAREHOUSE, toId=null}</li>
     * </ul>
     *
     * @param productionRunId  the production run the inventory belongs to
     * @param fromLocationType where inventory is coming from
     * @param fromLocationId   distributor ID when {@code fromLocationType} is DISTRIBUTOR;
     *                         null otherwise
     * @param toLocationType   where inventory is going to
     * @param toLocationId     distributor ID when {@code toLocationType} is DISTRIBUTOR;
     *                         null otherwise
     * @param quantity         number of units transferred (always positive)
     * @param movementType     the business event type (ALLOCATION, SALE, RETURN)
     * @param referenceId      optional ID of the sale or return that triggered this movement
     */
    void recordMovement(
            Long productionRunId,
            LocationType fromLocationType,
            Long fromLocationId,
            LocationType toLocationType,
            Long toLocationId,
            int quantity,
            MovementType movementType,
            Long referenceId
    );

    /**
     * Deletes all movement records that were created for a specific sale or return.
     *
     * <p>Used when editing or deleting a sale/return to reverse the inventory changes
     * before applying the new ones.
     *
     * @param movementType the type of movement to delete (SALE or RETURN)
     * @param referenceId  the ID of the sale or return whose movements should be deleted
     */
    void deleteMovementsByReference(MovementType movementType, Long referenceId);
}
