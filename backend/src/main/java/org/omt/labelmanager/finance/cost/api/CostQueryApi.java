package org.omt.labelmanager.finance.cost.api;

import java.util.List;
import org.omt.labelmanager.finance.cost.domain.Cost;

public interface CostQueryApi {

    List<Cost> getCostsForRelease(Long releaseId);

    List<Cost> getCostsForLabel(Long labelId);

    List<Cost> getCostsForUser(Long userId);
}
