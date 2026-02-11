package org.omt.labelmanager.inventory.allocation;

import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
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
    private final InventoryMovementRepository inventoryMovementRepository;
    private final ProductionRunRepository productionRunRepository;

    public AllocateProductionRunToDistributorUseCase(
            ChannelAllocationRepository channelAllocationRepository,
            InventoryMovementRepository inventoryMovementRepository,
            ProductionRunRepository productionRunRepository
    ) {
        this.channelAllocationRepository = channelAllocationRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.productionRunRepository = productionRunRepository;
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

        Instant now = Instant.now();

        ChannelAllocationEntity allocationEntity = new ChannelAllocationEntity(
                productionRunId,
                distributorId,
                quantity,
                now
        );
        allocationEntity = channelAllocationRepository.save(allocationEntity);
        log.debug("Allocation created with id {}", allocationEntity.getId());

        InventoryMovementEntity movementEntity = new InventoryMovementEntity(
                productionRunId,
                distributorId,
                quantity,
                MovementType.ALLOCATION,
                now,
                allocationEntity.getId()
        );
        inventoryMovementRepository.save(movementEntity);
        log.debug("Movement record created for allocation {}", allocationEntity.getId());

        return ChannelAllocation.fromEntity(allocationEntity);
    }

    private void validateQuantityIsAvailable(Long productionRunId, int quantity) {
        int manufactured = getManufacturedQuantity(productionRunId);
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

    private int getManufacturedQuantity(Long productionRunId) {
        return productionRunRepository.findById(productionRunId)
                .map(run -> run.getQuantity())
                .orElse(0);
    }
}
