package org.omt.labelmanager.inventory.allocation.application;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.omt.labelmanager.inventory.allocation.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.springframework.stereotype.Service;

@Service
class AllocationCommandApiImpl implements AllocationCommandApi {

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

        allocation.reduceQuantity(quantity);
        repository.save(allocation);
    }
}
