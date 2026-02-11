package org.omt.labelmanager.inventory.api;

import java.util.List;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;

public record ProductionRunWithAllocation(
        ProductionRun productionRun,
        int allocated,
        int unallocated,
        List<AllocationView> allocations
) {
}
