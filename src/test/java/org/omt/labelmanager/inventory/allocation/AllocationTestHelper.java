package org.omt.labelmanager.inventory.allocation;

import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.springframework.stereotype.Component;

/**
 * Test helper for creating allocation test fixtures.
 * Public component available for other modules' integration tests.
 */
@Component
public class AllocationTestHelper {

    private final AllocationCommandApi allocationCommandApi;

    public AllocationTestHelper(AllocationCommandApi allocationCommandApi) {
        this.allocationCommandApi = allocationCommandApi;
    }

    /**
     * Create an allocation for testing.
     *
     * @param productionRunId the production run ID
     * @param distributorId the distributor ID
     * @param quantity the quantity to allocate
     * @return the created allocation
     */
    public ChannelAllocation createAllocation(
            Long productionRunId,
            Long distributorId,
            int quantity
    ) {
        return allocationCommandApi.createAllocation(
                productionRunId,
                distributorId,
                quantity
        );
    }
}
