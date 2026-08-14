package org.omt.labelmanager.inventory.inventorymovement.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.inventory.inventorymovement.InventoryMovement;

/** Public API for inventory movement query operations. */
public interface InventoryMovementQueryApi {

    /**
     * Returns all inventory movements for a production run, sorted by {@code occurredAt}
     * descending.
     *
     * @param productionRunId the production run ID
     * @return movements, newest first
     */
    List<InventoryMovement> findByProductionRunId(Long productionRunId);

    /**
     * The same, for many runs in one query — for pages that show every pressing of a release.
     *
     * @param productionRunIds the production runs
     * @return movements by production run, newest first within each; runs with no movements are
     *     absent
     */
    Map<Long, List<InventoryMovement>> findByProductionRunIds(Collection<Long> productionRunIds);

    /**
     * Every non-zero location balance for these runs, summed in the database.
     *
     * <p>The one call that answers "how much is where" — the per-location methods below are
     * conveniences over it. Callers needing several runs, or several locations of one run, should
     * use this rather than calling them in a loop.
     *
     * @param productionRunIds the production runs; an empty collection returns an empty list
     * @return one entry per (run, location) holding a non-zero quantity
     */
    List<LocationBalance> balancesFor(Collection<Long> productionRunIds);

    /**
     * Calculates the current inventory held by a specific distributor for a production run.
     *
     * <p>Result = SUM(inbound to distributor) − SUM(outbound from distributor)
     *
     * @param productionRunId the production run
     * @param distributorId the distributor
     * @return current inventory quantity (may be 0 if fully sold or never allocated)
     */
    int getCurrentInventory(Long productionRunId, Long distributorId);

    /**
     * Calculates the current warehouse inventory for a production run.
     *
     * <p>Result = SUM(quantity moving TO warehouse) − SUM(quantity moving FROM warehouse).
     * Absolute, not a delta: manufacture is a PRODUCTION movement into the warehouse, so no caller
     * adds the run's quantity back in.
     *
     * @param productionRunId the production run
     * @return current warehouse inventory quantity
     */
    int getWarehouseInventory(Long productionRunId);

    /**
     * Returns the current quantity held by Bandcamp for a production run.
     *
     * <p>Result = SUM(ALLOCATION to BANDCAMP) − SUM(outbound from BANDCAMP)
     *
     * @param productionRunId the production run
     * @return current Bandcamp inventory quantity
     */
    int getBandcampInventory(Long productionRunId);

    /**
     * Returns distinct production run IDs that have at least one ALLOCATION movement to the given
     * distributor.
     *
     * @param distributorId the distributor
     * @return list of distinct production run IDs
     */
    List<Long> getProductionRunIdsAllocatedToDistributor(Long distributorId);
}
