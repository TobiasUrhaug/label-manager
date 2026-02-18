package org.omt.labelmanager.inventory.allocation.application;

import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class AllocationCommandApiImpl implements AllocationCommandApi {

    private static final Logger log =
            LoggerFactory.getLogger(AllocationCommandApiImpl.class);

    private final ChannelAllocationRepository repository;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final ProductionRunQueryApi productionRunQueryApi;

    AllocationCommandApiImpl(
            ChannelAllocationRepository repository,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            ProductionRunQueryApi productionRunQueryApi
    ) {
        this.repository = repository;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
        this.productionRunQueryApi = productionRunQueryApi;
    }

    @Override
    @Transactional
    public ChannelAllocation createAllocation(
            Long productionRunId,
            Long distributorId,
            int quantity
    ) {
        productionRunQueryApi.validateQuantityIsAvailable(productionRunId, quantity);

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
                LocationType.WAREHOUSE,
                null,
                LocationType.DISTRIBUTOR,
                distributorId,
                quantity,
                MovementType.ALLOCATION,
                allocation.id()
        );

        return allocation;
    }
}
