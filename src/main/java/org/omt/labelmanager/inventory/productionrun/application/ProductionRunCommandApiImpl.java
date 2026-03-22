package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
class ProductionRunCommandApiImpl implements ProductionRunCommandApi {

    private final CreateProductionRunUseCase createProductionRun;
    private final DeleteProductionRunUseCase deleteProductionRun;
    private final AllocateUseCase allocate;
    private final CancelBandcampReservationUseCase cancelBandcampReservation;

    ProductionRunCommandApiImpl(
            CreateProductionRunUseCase createProductionRun,
            DeleteProductionRunUseCase deleteProductionRun,
            AllocateUseCase allocate,
            CancelBandcampReservationUseCase cancelBandcampReservation
    ) {
        this.createProductionRun = createProductionRun;
        this.deleteProductionRun = deleteProductionRun;
        this.allocate = allocate;
        this.cancelBandcampReservation = cancelBandcampReservation;
    }

    @Override
    public ProductionRun createProductionRun(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {
        return createProductionRun.execute(
                releaseId,
                format,
                description,
                manufacturer,
                manufacturingDate,
                quantity
        );
    }

    @Override
    public boolean delete(Long id) {
        return deleteProductionRun.execute(id);
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
