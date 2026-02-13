package org.omt.labelmanager.inventory.productionrun;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Public helper for creating test production run data.
 * Used by integration tests in other modules that need production run
 * fixtures.
 */
@Component
public class ProductionRunTestHelper {

    private final ProductionRunRepository repository;

    public ProductionRunTestHelper(ProductionRunRepository repository) {
        this.repository = repository;
    }

    public ProductionRun createProductionRun(
            Long releaseId,
            ReleaseFormat format,
            int quantity
    ) {
        var entity = new ProductionRunEntity(
                releaseId,
                format,
                "Test pressing",
                "Test Manufacturer",
                LocalDate.now(),
                quantity
        );
        entity = repository.save(entity);
        return ProductionRun.fromEntity(entity);
    }

    public ProductionRun createProductionRun(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {
        var entity = new ProductionRunEntity(
                releaseId,
                format,
                description,
                manufacturer,
                manufacturingDate,
                quantity
        );
        entity = repository.save(entity);
        return ProductionRun.fromEntity(entity);
    }
}
