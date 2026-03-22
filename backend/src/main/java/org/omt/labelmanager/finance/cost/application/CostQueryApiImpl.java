package org.omt.labelmanager.finance.cost.application;

import org.omt.labelmanager.finance.cost.CostMapper;
import org.omt.labelmanager.finance.cost.infrastructure.CostRepository;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.finance.cost.domain.CostOwnerType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class CostQueryApiImpl implements CostQueryApi {

    private final CostRepository costRepository;

    CostQueryApiImpl(CostRepository costRepository) {
        this.costRepository = costRepository;
    }

    @Override
    public List<Cost> getCostsForRelease(Long releaseId) {
        return costRepository
                .findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType.RELEASE, releaseId)
                .stream()
                .map(CostMapper::fromEntity)
                .toList();
    }

    @Override
    public List<Cost> getCostsForLabel(Long labelId) {
        return costRepository
                .findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType.LABEL, labelId)
                .stream()
                .map(CostMapper::fromEntity)
                .toList();
    }

    @Override
    public List<Cost> getCostsForUser(Long userId) {
        return costRepository
                .findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType.USER, userId)
                .stream()
                .map(CostMapper::fromEntity)
                .toList();
    }
}
