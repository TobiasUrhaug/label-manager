package org.omt.labelmanager.inventory.allocation.application;

import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateAllocationUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(CreateAllocationUseCase.class);

    private final AllocationCommandApi allocationCommandApi;
    private final ProductionRunQueryApi productionRunQueryApi;

    CreateAllocationUseCase(
            AllocationCommandApi allocationCommandApi,
            ProductionRunQueryApi productionRunQueryApi
    ) {
        this.allocationCommandApi = allocationCommandApi;
        this.productionRunQueryApi = productionRunQueryApi;
    }

    @Transactional
    public ChannelAllocation execute(
            Long productionRunId,
            Long distributorId,
            int quantity
    ) {
        log.info(
                "Creating allocation of {} units from production run {} to distributor {}",
                quantity,
                productionRunId,
                distributorId
        );

        productionRunQueryApi.validateQuantityIsAvailable(productionRunId, quantity);
        return allocationCommandApi.createAllocation(productionRunId, distributorId, quantity);
    }
}
