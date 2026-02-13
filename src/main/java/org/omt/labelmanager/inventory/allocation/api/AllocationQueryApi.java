package org.omt.labelmanager.inventory.allocation.api;

import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;

import java.util.List;

/**
 * Public API for allocation query operations.
 */
public interface AllocationQueryApi {

    /**
     * Get all allocations for a specific production run.
     *
     * @param productionRunId the production run
     * @return list of allocations
     */
    List<ChannelAllocation> getAllocationsForProductionRun(Long productionRunId);

    /**
     * Get the total quantity allocated for a specific production run.
     *
     * @param productionRunId the production run
     * @return total allocated quantity
     */
    int getTotalAllocated(Long productionRunId);
}
