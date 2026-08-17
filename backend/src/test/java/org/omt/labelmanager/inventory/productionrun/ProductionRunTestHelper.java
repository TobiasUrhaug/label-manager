package org.omt.labelmanager.inventory.productionrun;

import java.time.LocalDate;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.shared.Format;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test production run data. Used by integration tests in other modules
 * that need production run fixtures.
 *
 * <p>Goes through {@link ProductionRunCommandApi} rather than saving an entity directly, so
 * fixtures carry the PRODUCTION movement a real run has. Saving the row alone produces a run the
 * application can no longer create: warehouse stock reads straight off the ledger now, so such a
 * run has zero stock and every allocation against it fails.
 */
@Component
public class ProductionRunTestHelper {

    private final ProductionRunCommandApi commandApi;

    public ProductionRunTestHelper(ProductionRunCommandApi commandApi) {
        this.commandApi = commandApi;
    }

    public ProductionRun createProductionRun(Long releaseId, Format format, int quantity) {
        return createProductionRun(
                releaseId, format, "Test pressing", "Test Manufacturer", LocalDate.now(), quantity);
    }

    public ProductionRun createProductionRun(
            Long releaseId,
            Format format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity) {
        return commandApi.createProductionRun(
                releaseId, format, description, manufacturer, manufacturingDate, quantity);
    }
}
