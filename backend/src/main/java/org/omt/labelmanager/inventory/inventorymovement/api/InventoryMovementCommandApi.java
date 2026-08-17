package org.omt.labelmanager.inventory.inventorymovement.api;

import java.time.LocalDate;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;

/** Public API for inventory movement command operations. */
public interface InventoryMovementCommandApi {

    /**
     * Records manufacture entering the warehouse: {@code external() → warehouse()}, typed {@code
     * PRODUCTION}.
     *
     * <p>Separate from {@link #recordMovement} because it is the only movement that occurred before
     * anyone recorded it — it is stamped with the manufacturing date, not the wall clock, so a run
     * created today for a pressing made last year sits in the right place in its own history. V33
     * backfills pre-existing runs the same way.
     *
     * @param productionRunId the production run that was manufactured
     * @param quantity units manufactured (always positive)
     * @param manufacturedOn the date the run was pressed
     */
    void recordManufacture(Long productionRunId, int quantity, LocalDate manufacturedOn);

    /**
     * Records a bidirectional inventory transfer.
     *
     * <p>Standard usage patterns:
     *
     * <ul>
     *   <li>Allocation: {@code warehouse() → distributor(id)}
     *   <li>Sale: {@code distributor(id) → external()}
     *   <li>Return: {@code distributor(id) → warehouse()}
     * </ul>
     *
     * <p>Manufacture goes through {@link #recordManufacture} instead.
     *
     * @param productionRunId the production run the inventory belongs to
     * @param from where inventory is coming from
     * @param to where inventory is going to
     * @param quantity number of units transferred (always positive)
     * @param movementType the business event type (ALLOCATION, SALE, RETURN)
     * @param referenceId optional ID of the sale or return that triggered this movement
     */
    void recordMovement(
            Long productionRunId,
            InventoryLocation from,
            InventoryLocation to,
            int quantity,
            MovementType movementType,
            Long referenceId);

    /**
     * Deletes all movement records that were created for a specific sale or return.
     *
     * <p>Used when editing or deleting a sale/return to reverse the inventory changes before
     * applying the new ones.
     *
     * @param movementType the type of movement to delete (SALE or RETURN)
     * @param referenceId the ID of the sale or return whose movements should be deleted
     */
    void deleteMovementsByReference(MovementType movementType, Long referenceId);
}
