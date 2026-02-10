package org.omt.labelmanager.finance.cost;

import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class CostQueryHandler implements CostQueryApi {

    private final CostQueryService costQueryService;

    CostQueryHandler(CostQueryService costQueryService) {
        this.costQueryService = costQueryService;
    }

    @Override
    public List<Cost> getCostsForRelease(Long releaseId) {
        return costQueryService.getCostsForRelease(releaseId);
    }

    @Override
    public List<Cost> getCostsForLabel(Long labelId) {
        return costQueryService.getCostsForLabel(labelId);
    }

    @Override
    public List<Cost> getCostsForUser(Long userId) {
        return costQueryService.getCostsForUser(userId);
    }
}
