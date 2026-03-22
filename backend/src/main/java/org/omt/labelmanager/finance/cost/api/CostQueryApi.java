package org.omt.labelmanager.finance.cost.api;

import org.omt.labelmanager.finance.cost.domain.Cost;

import java.util.List;

public interface CostQueryApi {

    List<Cost> getCostsForRelease(Long releaseId);

    List<Cost> getCostsForLabel(Long labelId);

    List<Cost> getCostsForUser(Long userId);

}
