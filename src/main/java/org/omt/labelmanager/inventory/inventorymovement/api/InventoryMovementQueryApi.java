package org.omt.labelmanager.inventory.inventorymovement.api;

import java.util.List;
import java.util.Map;
import org.omt.labelmanager.inventory.inventorymovement.domain.InventoryMovement;

/**
 * Public API for inventory movement query operations.
 */
public interface InventoryMovementQueryApi {

    /**
     * Returns all inventory movements for a production run, sorted by
     * {@code occurredAt} descending.
     *
     * @param productionRunId the production run ID
     * @return movements, newest first
     */
    List<InventoryMovement> findByProductionRunId(Long productionRunId);

    /**
     * Returns all inventory movements for a production run, sorted by
     * {@code occurredAt} descending.
     * Alias for {@link #findByProductionRunId(Long)} with a more descriptive name.
     *
     * @param productionRunId the production run ID
     * @return movements, newest first
     */
    List<InventoryMovement> getMovementsForProductionRun(Long productionRunId);

    /**
     * Calculates the current inventory held by a specific distributor for a production run.
     *
     * <p>Result = SUM(inbound to distributor) − SUM(outbound from distributor)
     *
     * @param productionRunId the production run
     * @param distributorId   the distributor
     * @return current inventory quantity (may be 0 if fully sold or never allocated)
     */
    int getCurrentInventory(Long productionRunId, Long distributorId);

    /**
     * Calculates the current warehouse inventory for a production run.
     *
     * <p>Result = SUM(quantity moving TO warehouse) − SUM(quantity moving FROM warehouse)
     *
     * @param productionRunId the production run
     * @return current warehouse inventory quantity
     */
    int getWarehouseInventory(Long productionRunId);

    /**
     * Returns current inventory per distributor for a production run.
     *
     * <p>Distributors with zero current inventory may be omitted from the result.
     *
     * @param productionRunId the production run
     * @return map of distributorId → current quantity
     */
    Map<Long, Integer> getCurrentInventoryByDistributor(Long productionRunId);
}
