package org.omt.labelmanager.inventory.productionrun.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;

public interface ProductionRunQueryApi {

    Optional<ProductionRun> findById(Long productionRunId);

    List<ProductionRun> findByReleaseId(Long releaseId);

    /**
     * Find the most recent production run for a release/format combination. Used to determine which
     * pressing to sell from.
     */
    Optional<ProductionRun> findMostRecent(Long releaseId, ReleaseFormat format);

    /** Returns the quantity manufactured for this production run. */
    int getManufacturedQuantity(Long productionRunId);
}
