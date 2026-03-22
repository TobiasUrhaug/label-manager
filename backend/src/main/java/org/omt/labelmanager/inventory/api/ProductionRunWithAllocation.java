package org.omt.labelmanager.inventory.api;

import java.util.List;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;

/**
 * View model combining a production run with its inventory data
 * for display on the release detail page.
 */
public record ProductionRunWithAllocation(
        ProductionRun productionRun,
        int bandcampInventory,
        int warehouseInventory,
        List<DistributorInventoryView> distributorInventories,
        List<MovementHistoryView> movements
) {
}
