package org.omt.labelmanager.inventory.allocation.application;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReduceAllocationUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(ReduceAllocationUseCase.class);

    private final ChannelAllocationRepository repository;

    ReduceAllocationUseCase(ChannelAllocationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long productionRunId, Long distributorId, int quantity) {
        log.info(
                "Reducing allocation by {} units for production run {} and distributor {}",
                quantity,
                productionRunId,
                distributorId
        );

        var allocation = repository
                .findByProductionRunIdAndDistributorId(productionRunId, distributorId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No allocation found for production run " + productionRunId
                                + " and distributor " + distributorId
                ));

        allocation.incrementUnitsSold(quantity);
        repository.save(allocation);

        log.debug(
                "Allocation {} updated: units sold increased by {}",
                allocation.getId(),
                quantity
        );
    }
}
