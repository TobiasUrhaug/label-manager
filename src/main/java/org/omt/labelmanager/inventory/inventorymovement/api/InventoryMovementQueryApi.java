package org.omt.labelmanager.inventory.inventorymovement.api;

import java.util.List;
import org.omt.labelmanager.inventory.inventorymovement.domain.InventoryMovement;

/**
 * Public API for inventory movement query operations.
 */
public interface InventoryMovementQueryApi {

    /**
     * Find all inventory movements for a production run.
     *
     * @param productionRunId the production run ID
     * @return list of movements
     */
    List<InventoryMovement> findByProductionRunId(Long productionRunId);

    /**
     * Find all inventory movements for a distributor.
     *
     * @param distributorId the distributor ID
     * @return list of movements
     */
    List<InventoryMovement> findByDistributorId(Long distributorId);
}
