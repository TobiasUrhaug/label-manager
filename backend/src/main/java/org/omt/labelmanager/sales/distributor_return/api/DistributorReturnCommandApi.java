package org.omt.labelmanager.sales.distributor_return.api;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;

/**
 * Public command interface for creating, editing, and deleting distributor returns.
 */
public interface DistributorReturnCommandApi {

    /**
     * Registers a new return of physical inventory from a distributor back to the warehouse.
     * For each line item, validates that the distributor currently holds enough inventory
     * before recording RETURN movements.
     *
     * @param labelId      the label the return belongs to
     * @param distributorId the distributor returning the inventory
     * @param returnDate   the date of the return
     * @param notes        optional notes
     * @param lineItems    one or more line items specifying what is being returned
     * @return the persisted return
     * @throws IllegalArgumentException if lineItems is empty or the distributor is not
     *                                  found under the label
     * @throws org.omt.labelmanager.inventory.InsufficientInventoryException if the
     *                                  distributor does not hold enough inventory for
     *                                  any of the line items
     */
    DistributorReturn registerReturn(
            Long labelId,
            Long distributorId,
            LocalDate returnDate,
            String notes,
            List<ReturnLineItemInput> lineItems
    );

    /**
     * Updates a return's date, notes, and line items. The distributor is immutable
     * after registration. Old RETURN movements are deleted and new ones are recorded.
     *
     * @param returnId   the return to update
     * @param returnDate new date
     * @param notes      new notes (may be null)
     * @param lineItems  new line items
     * @throws jakarta.persistence.EntityNotFoundException if the return does not exist
     */
    void updateReturn(
            Long returnId,
            LocalDate returnDate,
            String notes,
            List<ReturnLineItemInput> lineItems
    );

    /**
     * Deletes a return and reverses all associated RETURN inventory movements.
     *
     * @param returnId the return to delete
     * @throws jakarta.persistence.EntityNotFoundException if the return does not exist
     */
    void deleteReturn(Long returnId);
}
