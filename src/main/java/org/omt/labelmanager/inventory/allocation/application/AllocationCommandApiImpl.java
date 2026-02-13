package org.omt.labelmanager.inventory.allocation.application;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.omt.labelmanager.inventory.allocation.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class AllocationCommandApiImpl implements AllocationCommandApi {

    private static final Logger log =
            LoggerFactory.getLogger(AllocationCommandApiImpl.class);

    private final ChannelAllocationRepository repository;

    AllocationCommandApiImpl(ChannelAllocationRepository repository) {
        this.repository = repository;
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
    public ChannelAllocation createAllocation(Long productionRunId, Long distributorId, int quantity) {
        ChannelAllocationEntity allocationEntity = new ChannelAllocationEntity(
                productionRunId,
                distributorId,
                quantity,
                Instant.now()
        );
        allocationEntity = repository.save(allocationEntity);
        log.debug("Allocation created with id {}", allocationEntity.getId());
        return ChannelAllocation.fromEntity(allocationEntity);
    }
}
