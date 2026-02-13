package org.omt.labelmanager.inventory.allocation.application;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class AllocationCommandApiImpl implements AllocationCommandApi {

    private static final Logger log =
            LoggerFactory.getLogger(AllocationCommandApiImpl.class);

    private final ChannelAllocationRepository repository;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    AllocationCommandApiImpl(
            ChannelAllocationRepository repository,
            InventoryMovementCommandApi inventoryMovementCommandApi
    ) {
        this.repository = repository;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Override
    @Transactional
    public void reduceAllocation(Long productionRunId, Long distributorId, int quantity) {
        var allocation = repository
                .findByProductionRunIdAndDistributorId(productionRunId, distributorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No allocation found for production run " + productionRunId
                                + " and distributor " + distributorId
                ));

        allocation.incrementUnitsSold(quantity);
        repository.save(allocation);
    }

    @Override
    @Transactional
    public ChannelAllocation createAllocation(Long productionRunId, Long distributorId, int quantity) {
        ChannelAllocationEntity allocationEntity = new ChannelAllocationEntity(
                productionRunId,
                distributorId,
                quantity,
                Instant.now()
        );
        allocationEntity = repository.save(allocationEntity);
        log.debug("Allocation created with id {}", allocationEntity.getId());

        ChannelAllocation allocation = ChannelAllocation.fromEntity(allocationEntity);
        inventoryMovementCommandApi.recordMovement(
                productionRunId,
                distributorId,
                quantity,
                MovementType.ALLOCATION,
                allocation.id()
        );

        return allocation;
    }
}
