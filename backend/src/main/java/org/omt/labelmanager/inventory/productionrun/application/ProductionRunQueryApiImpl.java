package org.omt.labelmanager.inventory.productionrun.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;
import org.omt.labelmanager.shared.Format;
import org.springframework.stereotype.Service;

@Service
class ProductionRunQueryApiImpl implements ProductionRunQueryApi {

    private final ProductionRunRepository repository;

    ProductionRunQueryApiImpl(ProductionRunRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ProductionRun> findById(Long productionRunId) {
        return repository.findById(productionRunId).map(ProductionRun::fromEntity);
    }

    @Override
    public List<ProductionRun> findByReleaseId(Long releaseId) {
        return repository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

    @Override
    public List<ProductionRun> findByReleaseIds(Collection<Long> releaseIds) {
        if (releaseIds.isEmpty()) {
            return List.of();
        }
        return repository.findByReleaseIdIn(releaseIds).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

    @Override
    public Optional<ProductionRun> findMostRecent(Long releaseId, Format format) {
        return repository
                .findTopByReleaseIdAndFormatOrderByManufacturingDateDesc(releaseId, format)
                .map(ProductionRun::fromEntity);
    }
}
