package org.omt.labelmanager.inventory.allocation;

import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AllocateProductionRunToDistributorUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(AllocateProductionRunToDistributorUseCase.class);

    private final AllocationCommandApi allocationCommandApi;
    private final AllocationQueryService allocationQueryService;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final ProductionRunQueryApi productionRunQueryApi;

    public AllocateProductionRunToDistributorUseCase(
            AllocationCommandApi allocationCommandApi, AllocationQueryService allocationQueryService,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            ProductionRunQueryApi productionRunQueryApi
    ) {
        this.allocationCommandApi = allocationCommandApi;
        this.allocationQueryService = allocationQueryService;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
        this.productionRunQueryApi = productionRunQueryApi;
    }

    @Transactional
    public ChannelAllocation invoke(
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

        validateQuantityIsAvailable(productionRunId, quantity);

        ChannelAllocation allocation = allocationCommandApi.createAllocation(productionRunId, distributorId, quantity);
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                distributorId,
                quantity,
                MovementType.ALLOCATION,
                allocation.id()
        );

        return allocation;
    }

    private void validateQuantityIsAvailable(Long productionRunId, int quantity) {
        int manufactured = productionRunQueryApi.getManufacturedQuantity(productionRunId);
        int allocated = allocationQueryService.getTotalAllocated(productionRunId);
        int unallocated = manufactured - allocated;

        if (quantity > unallocated) {
            log.warn(
                    "Allocation rejected: requested {} but only {} unallocated for run {}",
                    quantity,
                    unallocated,
                    productionRunId
            );
            throw new InsufficientInventoryException(quantity, unallocated);
        }
    }
}
