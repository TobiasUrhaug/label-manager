package org.omt.labelmanager.productionrun.application;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.productionrun.domain.ProductionRun;
import org.omt.labelmanager.productionrun.infrastructure.persistence.ProductionRunEntity;
import org.omt.labelmanager.productionrun.infrastructure.persistence.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionRunCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductionRunCRUDHandler.class);

    private final ProductionRunRepository productionRunRepository;

    public ProductionRunCRUDHandler(ProductionRunRepository productionRunRepository) {
        this.productionRunRepository = productionRunRepository;
    }

    @Transactional
    public ProductionRun create(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {
        log.info("Creating production run for release {} - {} x{}", releaseId, format, quantity);

        ProductionRunEntity entity = new ProductionRunEntity(
                releaseId,
                format,
                description,
                manufacturer,
                manufacturingDate,
                quantity
        );

        entity = productionRunRepository.save(entity);
        log.debug("Production run created with id {}", entity.getId());

        return ProductionRun.fromEntity(entity);
    }

    public List<ProductionRun> findByReleaseId(Long releaseId) {
        return productionRunRepository.findByReleaseId(releaseId).stream()
                .map(ProductionRun::fromEntity)
                .toList();
    }

    @Transactional
    public boolean delete(Long id) {
        if (!productionRunRepository.existsById(id)) {
            log.warn("Production run with id {} not found for deletion", id);
            return false;
        }

        productionRunRepository.deleteById(id);
        log.info("Deleted production run with id {}", id);
        return true;
    }
}
