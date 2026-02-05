package org.omt.labelmanager.inventory.domain;

import java.time.LocalDate;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryEntity;

public record Inventory(
        Long id,
        Long releaseId,
        ReleaseFormat format,
        String description,
        String manufacturer,
        LocalDate manufacturingDate,
        int quantity
) {

    public static Inventory fromEntity(InventoryEntity entity) {
        return new Inventory(
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
