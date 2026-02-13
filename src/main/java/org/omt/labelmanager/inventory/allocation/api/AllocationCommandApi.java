package org.omt.labelmanager.inventory.allocation.api;

import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;

/**
 * Public API for allocation command operations.
 */
public interface AllocationCommandApi {

    /**
     * Reduce the allocation quantity for a specific production run and distributor.
     * Used when inventory is sold or transferred.
     *
     * @param productionRunId the production run
     * @param distributorId the distributor
     * @param quantity the amount to reduce
     * @throws IllegalStateException if insufficient quantity available
     */
    void reduceAllocation(Long productionRunId, Long distributorId, int quantity);

    /**
     * Add the allocation quantity for a specific production run to distributor.
     * Used when inventory is allocated to a distributor.
     *
     * @param productionRunId the production run
     * @param distributorId the distributor
     * @param quantity the amount to allocate
     * @throws IllegalStateException if insufficient quantity available
     */
    ChannelAllocation createAllocation(Long productionRunId, Long distributorId, int quantity);
}
