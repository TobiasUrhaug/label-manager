package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
class ProductionRunCommandApiImpl implements ProductionRunCommandApi {

    private static final Logger log = LoggerFactory.getLogger(ProductionRunCommandApiImpl.class);

    private final ProductionRunRepository repository;
    private final AllocateUseCase allocate;
    private final CancelBandcampReservationUseCase cancelBandcampReservation;

    ProductionRunCommandApiImpl(
            ProductionRunRepository repository,
            AllocateUseCase allocate,
            CancelBandcampReservationUseCase cancelBandcampReservation
    ) {
        this.repository = repository;
        this.allocate = allocate;
        this.cancelBandcampReservation = cancelBandcampReservation;
    }

    @Override
    @Transactional
    public ProductionRun createProductionRun(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {
        log.info("Creating production run for release {} - {} x{}", releaseId, format, quantity);
        ProductionRunEntity entity = new ProductionRunEntity(
                releaseId, format, description, manufacturer, manufacturingDate, quantity);
        entity = repository.save(entity);
        log.debug("Production run created with id {}", entity.getId());
        return ProductionRun.fromEntity(entity);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            log.warn("Production run with id {} not found for deletion", id);
            return false;
        }
        repository.deleteById(id);
        log.info("Deleted production run with id {}", id);
        return true;
    }

    @Override
    public void allocate(Long productionRunId, InventoryLocation toLocation, int quantity) {
        allocate.execute(productionRunId, toLocation, quantity);
    }

    @Override
    public void cancelBandcampReservation(Long productionRunId, int quantity) {
        cancelBandcampReservation.execute(productionRunId, quantity);
    }
}
