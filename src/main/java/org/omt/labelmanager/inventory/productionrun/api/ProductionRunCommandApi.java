package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;

import java.time.LocalDate;

public interface ProductionRunCommandApi {

    ProductionRun createProductionRun(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    );

    boolean delete(Long id);
}
