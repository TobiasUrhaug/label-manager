package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.domain.InventoryLocation;
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

    /**
     * Allocates units from the production run's warehouse stock to the given location.
     *
     * @throws org.omt.labelmanager.inventory.InsufficientInventoryException if requested
     *         quantity exceeds available warehouse stock
     */
    void allocate(Long productionRunId, InventoryLocation toLocation, int quantity);

    /**
     * Cancels a Bandcamp reservation by recording a RETURN movement from BANDCAMP to WAREHOUSE.
     *
     * @throws org.omt.labelmanager.inventory.InsufficientInventoryException if requested
     *         quantity exceeds units currently held by Bandcamp
     */
    void cancelBandcampReservation(Long productionRunId, int quantity);
}
