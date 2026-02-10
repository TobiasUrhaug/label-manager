package org.omt.labelmanager.finance.cost.api;

import org.omt.labelmanager.finance.cost.Cost;

import java.util.List;

public interface CostQueryFacade {

    List<Cost> getCostsForRelease(Long releaseId);

    List<Cost> getCostsForLabel(Long labelId);

    List<Cost> getCostsForUser(Long userId);

}
