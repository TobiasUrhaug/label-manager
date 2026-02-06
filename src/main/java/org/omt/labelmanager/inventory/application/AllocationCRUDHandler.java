package org.omt.labelmanager.inventory.application;

import java.time.Instant;
import org.omt.labelmanager.inventory.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.infrastructure.persistence.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryMovementRepository;
import org.omt.labelmanager.inventory.infrastructure.persistence.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AllocationCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(AllocationCRUDHandler.class);

    private final ChannelAllocationRepository channelAllocationRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final ProductionRunRepository productionRunRepository;

    public AllocationCRUDHandler(
            ChannelAllocationRepository channelAllocationRepository,
            InventoryMovementRepository inventoryMovementRepository,
            ProductionRunRepository productionRunRepository
    ) {
        this.channelAllocationRepository = channelAllocationRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.productionRunRepository = productionRunRepository;
    }

    @Transactional
    public ChannelAllocation create(Long productionRunId, Long salesChannelId, int quantity) {
        log.info(
                "Creating allocation of {} units from production run {} to channel {}",
                quantity,
                productionRunId,
                salesChannelId
        );

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

    private int getManufacturedQuantity(Long productionRunId) {
        return productionRunRepository.findById(productionRunId)
                .map(run -> run.getQuantity())
                .orElse(0);
    }
}
