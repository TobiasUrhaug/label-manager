package org.omt.labelmanager.inventory.allocation;

import org.omt.labelmanager.inventory.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AllocateProductionRunToDistributorUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(AllocateProductionRunToDistributorUseCase.class);

    private final ChannelAllocationRepository channelAllocationRepository;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final ProductionRunQueryApi productionRunQueryApi;

    public AllocateProductionRunToDistributorUseCase(
            ChannelAllocationRepository channelAllocationRepository,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            ProductionRunQueryApi productionRunQueryApi
    ) {
        this.channelAllocationRepository = channelAllocationRepository;
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
        ChannelAllocationEntity allocationEntity = new ChannelAllocationEntity(
                productionRunId,
                distributorId,
                quantity,
                Instant.now()
        );
        allocationEntity = channelAllocationRepository.save(allocationEntity);
        log.debug("Allocation created with id {}", allocationEntity.getId());

        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                distributorId,
                quantity,
                MovementType.ALLOCATION,
                allocationEntity.getId()
        );

        return ChannelAllocation.fromEntity(allocationEntity);
    }

    private void validateQuantityIsAvailable(Long productionRunId, int quantity) {
        int manufactured = productionRunQueryApi.getManufacturedQuantity(productionRunId);
        int allocated = channelAllocationRepository.sumQuantityByProductionRunId(productionRunId);
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
