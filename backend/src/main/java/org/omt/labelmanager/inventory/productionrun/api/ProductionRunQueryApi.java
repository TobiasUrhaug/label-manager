package org.omt.labelmanager.inventory.productionrun.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.shared.Format;

public interface ProductionRunQueryApi {

    Optional<ProductionRun> findById(Long productionRunId);

    List<ProductionRun> findByReleaseId(Long releaseId);

    /**
     * Every production run for any of these releases, in one query.
     *
     * @param releaseIds the release ids; an empty collection returns an empty list
     * @return the matching production runs
     */
    List<ProductionRun> findByReleaseIds(Collection<Long> releaseIds);

    /**
     * Find the most recent production run for a release/format combination. Used to determine which
     * pressing to sell from.
     */
    Optional<ProductionRun> findMostRecent(Long releaseId, Format format);
}
