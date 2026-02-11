package org.omt.labelmanager.inventory.allocation;

import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AllocationQueryService {

    private final ChannelAllocationRepository channelAllocationRepository;
    private final ProductionRunRepository productionRunRepository;

    public AllocationQueryService(
            ChannelAllocationRepository channelAllocationRepository,
            ProductionRunRepository productionRunRepository
    ) {
        this.channelAllocationRepository = channelAllocationRepository;
        this.productionRunRepository = productionRunRepository;
    }

    public List<ChannelAllocation> getAllocationsForProductionRun(Long productionRunId) {
        return channelAllocationRepository.findByProductionRunId(productionRunId).stream()
                .map(ChannelAllocation::fromEntity)
                .toList();
    }

    public int getTotalAllocated(Long productionRunId) {
        return channelAllocationRepository.sumQuantityByProductionRunId(productionRunId);
    }

    public int getUnallocatedQuantity(Long productionRunId) {
        int manufactured = productionRunRepository.findById(productionRunId)
                .map(run -> run.getQuantity())
                .orElse(0);
        int allocated = getTotalAllocated(productionRunId);
        return manufactured - allocated;
    }
}
