package org.omt.labelmanager.inventory.allocation.api;

import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;

/**
 * Public API for allocation command operations.
 */
public interface AllocationCommandApi {

    /**
     * Allocates inventory from the warehouse to a distributor.
     *
     * @param productionRunId the production run
     * @param distributorId the distributor
     * @param quantity the amount to allocate
     * @throws IllegalStateException if insufficient quantity available in the production run
     */
    ChannelAllocation createAllocation(Long productionRunId, Long distributorId, int quantity);
}
