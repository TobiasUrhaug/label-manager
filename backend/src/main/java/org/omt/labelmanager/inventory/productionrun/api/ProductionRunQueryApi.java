package org.omt.labelmanager.inventory.productionrun.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.shared.Format;

public interface ProductionRunQueryApi {

    Optional<ProductionRun> findById(Long productionRunId);

    List<ProductionRun> findByReleaseId(Long releaseId);

    /**
     * Find the most recent production run for a release/format combination. Used to determine which
     * pressing to sell from.
     */
    Optional<ProductionRun> findMostRecent(Long releaseId, Format format);

    /** Returns the quantity manufactured for this production run. */
    int getManufacturedQuantity(Long productionRunId);
}
