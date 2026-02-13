package org.omt.labelmanager.inventory.allocation.application;

import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class AllocationQueryApiImpl implements AllocationQueryApi {

    private final ChannelAllocationRepository channelAllocationRepository;

    AllocationQueryApiImpl(ChannelAllocationRepository channelAllocationRepository) {
        this.channelAllocationRepository = channelAllocationRepository;
    }

    @Override
    public List<ChannelAllocation> getAllocationsForProductionRun(Long productionRunId) {
        return channelAllocationRepository.findByProductionRunId(productionRunId).stream()
                .map(ChannelAllocation::fromEntity)
                .toList();
    }

    @Override
    public int getTotalAllocated(Long productionRunId) {
        return channelAllocationRepository.sumQuantityByProductionRunId(productionRunId);
    }
}
