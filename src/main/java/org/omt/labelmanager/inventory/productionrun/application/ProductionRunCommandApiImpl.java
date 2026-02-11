package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
class ProductionRunCommandApiImpl implements ProductionRunCommandApi {

    private final CreateProductionRunUseCase createProductionRun;
    private final DeleteProductionRunUseCase deleteProductionRun;

    ProductionRunCommandApiImpl(
            CreateProductionRunUseCase createProductionRun,
            DeleteProductionRunUseCase deleteProductionRun
    ) {
        this.createProductionRun = createProductionRun;
        this.deleteProductionRun = deleteProductionRun;
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
}
