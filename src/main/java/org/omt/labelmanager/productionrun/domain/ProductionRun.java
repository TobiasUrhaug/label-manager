package org.omt.labelmanager.productionrun.domain;

import java.time.LocalDate;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.productionrun.infrastructure.persistence.ProductionRunEntity;

public record ProductionRun(
        Long id,
        Long releaseId,
        ReleaseFormat format,
        String description,
        String manufacturer,
        LocalDate manufacturingDate,
        int quantity
) {

    public static ProductionRun fromEntity(ProductionRunEntity entity) {
        return new ProductionRun(
                entity.getId(),
                entity.getReleaseId(),
                entity.getFormat(),
                entity.getDescription(),
                entity.getManufacturer(),
                entity.getManufacturingDate(),
                entity.getQuantity()
        );
    }
}
