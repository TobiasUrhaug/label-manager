package org.omt.labelmanager.productionrun.application;

import org.omt.labelmanager.productionrun.domain.ProductionRun;
import org.omt.labelmanager.productionrun.infrastructure.persistence.ProductionRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductionRunQueryService {

    private final ProductionRunRepository productionRunRepository;

    public ProductionRunQueryService(ProductionRunRepository productionRunRepository) {
        this.productionRunRepository = productionRunRepository;
    }

    public List<ProductionRun> getProductionRunsForRelease(Long releaseId) {
        return productionRunRepository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

}
