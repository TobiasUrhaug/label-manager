package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.allocation.domain.InsufficientInventoryException;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class ProductionRunQueryApiImpl implements ProductionRunQueryApi {

    private static final Logger log =
            LoggerFactory.getLogger(ProductionRunQueryApiImpl.class);

    private final ProductionRunRepository repository;
    private final AllocationQueryApi allocationQueryService;

    ProductionRunQueryApiImpl(ProductionRunRepository repository, AllocationQueryApi allocationQueryService) {
        this.repository = repository;
        this.allocationQueryService = allocationQueryService;
    }

    @Override
    public List<ProductionRun> findByReleaseId(Long releaseId) {
        return repository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

    @Override
    public Optional<ProductionRun> findMostRecent(Long releaseId, ReleaseFormat format) {
        return repository
                .findTopByReleaseIdAndFormatOrderByManufacturingDateDesc(releaseId, format)
                .map(ProductionRun::fromEntity);
    }

    @Override
    public int getManufacturedQuantity(Long productionRunId) {
        return repository.findById(productionRunId)
                .map(ProductionRunEntity::getQuantity)
                .orElse(0);
    }

    @Override
    public void validateQuantityIsAvailable(Long productionRunId, int quantity) {
        ProductionRun productionRun = repository.findById(productionRunId)
                .map(ProductionRun::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Production run not found: " + productionRunId
                ));

        int allocated = allocationQueryService.getTotalAllocated(productionRunId);

        if (!productionRun.canAllocate(quantity, allocated)) {
            int available = productionRun.getAvailableQuantity(allocated);
            log.warn(
                    "Allocation rejected: requested {} but only {} available for run {}",
                    quantity,
                    available,
                    productionRunId
            );
            throw new InsufficientInventoryException(quantity, available);
        }
    }
}
