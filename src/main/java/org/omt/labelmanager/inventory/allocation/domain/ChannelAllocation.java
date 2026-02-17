package org.omt.labelmanager.inventory.allocation.domain;

import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationEntity;

import java.time.Instant;

public record ChannelAllocation(
        Long id,
        Long productionRunId,
        Long distributorId,
        int quantity,
        Instant allocatedAt
) {

    public static ChannelAllocation fromEntity(ChannelAllocationEntity entity) {
        return new ChannelAllocation(
                entity.getId(),
                entity.getProductionRunId(),
                entity.getDistributorId(),
                entity.getQuantity(),
                entity.getAllocatedAt()
        );
    }
}
