package org.omt.labelmanager.finance.cost.application;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.finance.cost.CostMapper;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.finance.cost.domain.CostOwnerType;
import org.omt.labelmanager.finance.cost.infrastructure.CostRepository;
import org.springframework.stereotype.Service;

@Service
class CostQueryApiImpl implements CostQueryApi {

    private final CostRepository costRepository;

    CostQueryApiImpl(CostRepository costRepository) {
        this.costRepository = costRepository;
    }

    @Override
    public Optional<Cost> findById(Long costId) {
        return costRepository.findById(costId).map(CostMapper::fromEntity);
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
