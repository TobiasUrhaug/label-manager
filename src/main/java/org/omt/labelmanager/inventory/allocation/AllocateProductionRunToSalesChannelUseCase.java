package org.omt.labelmanager.inventory.allocation;

import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.omt.labelmanager.inventory.infrastructure.persistence.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AllocateProductionRunToSalesChannelUseCase {

    private static final Logger log = LoggerFactory.getLogger(AllocateProductionRunToSalesChannelUseCase.class);

    private final ChannelAllocationRepository channelAllocationRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final ProductionRunRepository productionRunRepository;

    public AllocateProductionRunToSalesChannelUseCase(
            ChannelAllocationRepository channelAllocationRepository,
            InventoryMovementRepository inventoryMovementRepository,
            ProductionRunRepository productionRunRepository
    ) {
        this.channelAllocationRepository = channelAllocationRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.productionRunRepository = productionRunRepository;
    }

    @Transactional
    public ChannelAllocation invoke(Long productionRunId, Long salesChannelId, int quantity) {
        log.info(
                "Creating allocation of {} units from production run {} to channel {}",
                quantity,
                productionRunId,
                salesChannelId
        );

        validateQuantityIsAvailable(productionRunId, quantity);

        Instant now = Instant.now();

        ChannelAllocationEntity allocationEntity = new ChannelAllocationEntity(
                productionRunId,
                salesChannelId,
                quantity,
                now
        );
        allocationEntity = channelAllocationRepository.save(allocationEntity);
        log.debug("Allocation created with id {}", allocationEntity.getId());

        InventoryMovementEntity movementEntity = new InventoryMovementEntity(
                productionRunId,
                salesChannelId,
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
