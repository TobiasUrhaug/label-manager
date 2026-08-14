package org.omt.labelmanager.finance.cost.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.finance.cost.domain.Cost;

public interface CostQueryApi {

    /**
     * Finds a single cost, whatever it is owned by.
     *
     * @param costId the cost id
     * @return the cost, or empty if no cost has that id
     */
    Optional<Cost> findById(Long costId);

    List<Cost> getCostsForRelease(Long releaseId);

    List<Cost> getCostsForLabel(Long labelId);

    List<Cost> getCostsForUser(Long userId);
}
