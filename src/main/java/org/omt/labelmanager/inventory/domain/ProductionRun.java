package org.omt.labelmanager.inventory.domain;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.infrastructure.persistence.ProductionRunEntity;

import java.time.LocalDate;

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
