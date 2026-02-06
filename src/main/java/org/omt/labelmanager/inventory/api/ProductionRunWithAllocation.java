package org.omt.labelmanager.inventory.api;

import org.omt.labelmanager.inventory.domain.ProductionRun;

public record ProductionRunWithAllocation(
        ProductionRun productionRun,
        int allocated,
        int unallocated
) {
}
