package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
class CreateProductionRunUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(CreateProductionRunUseCase.class);

    private final ProductionRunRepository repository;

    CreateProductionRunUseCase(ProductionRunRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductionRun execute(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {
        log.info(
                "Creating production run for release {} - {} x{}",
                releaseId,
                format,
                quantity
        );

        ProductionRunEntity entity = new ProductionRunEntity(
                releaseId,
                format,
                description,
                manufacturer,
                manufacturingDate,
                quantity
        );

        entity = repository.save(entity);
        log.debug("Production run created with id {}", entity.getId());

        return ProductionRun.fromEntity(entity);
    }
}
