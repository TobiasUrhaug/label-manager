package org.omt.labelmanager.inventory.productionrun.domain;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;

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

    public boolean canAllocate(int requestedQuantity, int currentlyAllocated) {
        int available = quantity - currentlyAllocated;
        return requestedQuantity <= available;
    }

    public int getAvailableQuantity(int currentlyAllocated) {
        return quantity - currentlyAllocated;
    }
}
