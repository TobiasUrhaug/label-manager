package org.omt.labelmanager.finance.cost;

import java.util.List;
import org.omt.labelmanager.finance.cost.persistence.CostRepository;
import org.springframework.stereotype.Service;

@Service
public class CostQueryService {

    private final CostRepository costRepository;

    public CostQueryService(CostRepository costRepository) {
        this.costRepository = costRepository;
    }

    public List<Cost> getCostsForRelease(Long releaseId) {
        return costRepository
                .findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType.RELEASE, releaseId)
                .stream()
                .map(Cost::fromEntity)
                .toList();
    }

    public List<Cost> getCostsForLabel(Long labelId) {
        return costRepository
                .findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType.LABEL, labelId)
                .stream()
                .map(Cost::fromEntity)
                .toList();
    }

    public List<Cost> getCostsForUser(Long userId) {
        return costRepository
                .findByOwnerOwnerTypeAndOwnerOwnerId(CostOwnerType.USER, userId)
                .stream()
                .map(Cost::fromEntity)
                .toList();
    }
}
