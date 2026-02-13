package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;

import java.util.List;
import java.util.Optional;

public interface ProductionRunQueryApi {

    List<ProductionRun> findByReleaseId(Long releaseId);

    /**
     * Find the most recent production run for a release/format combination.
     * Used to determine which pressing to sell from.
     */
    Optional<ProductionRun> findMostRecent(Long releaseId, ReleaseFormat format);

    /**
     * Get the quantity manufactured for this production run
     */
    int getManufacturedQuantity(Long productionRunId);

    /**
     * Validates that the requested quantity is available for allocation from a production run.
     * Checks that unallocated inventory (manufactured minus already allocated) is sufficient.
     *
     * @param productionRunId the production run to check inventory for
     * @param quantity the quantity requested for allocation
     * @throws InsufficientInventoryException if requested quantity exceeds unallocated inventory
     */
    void validateQuantityIsAvailable(Long productionRunId, int quantity);
}
