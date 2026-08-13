package org.omt.labelmanager.inventory.productionrun;

import java.time.LocalDate;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;
import org.omt.labelmanager.shared.Format;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test production run data. Used by integration tests in other modules
 * that need production run fixtures.
 */
@Component
public class ProductionRunTestHelper {

    private final ProductionRunRepository repository;

    public ProductionRunTestHelper(ProductionRunRepository repository) {
        this.repository = repository;
    }

    public ProductionRun createProductionRun(Long releaseId, Format format, int quantity) {
        var entity =
                new ProductionRunEntity(
                        releaseId,
                        format,
                        "Test pressing",
                        "Test Manufacturer",
                        LocalDate.now(),
                        quantity);
        entity = repository.save(entity);
        return ProductionRun.fromEntity(entity);
    }

    public ProductionRun createProductionRun(
            Long releaseId,
            Format format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity) {
        var entity =
                new ProductionRunEntity(
                        releaseId, format, description, manufacturer, manufacturingDate, quantity);
        entity = repository.save(entity);
        return ProductionRun.fromEntity(entity);
    }
}
