package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ProductionRunQueryApiImpl implements ProductionRunQueryApi {

    private final ProductionRunRepository repository;

    ProductionRunQueryApiImpl(ProductionRunRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductionRun> findByReleaseId(Long releaseId) {
        return repository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }
}
