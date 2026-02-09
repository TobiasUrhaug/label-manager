package org.omt.labelmanager.inventory.allocation;

import java.time.Instant;

public record ChannelAllocation(
        Long id,
        Long productionRunId,
        Long salesChannelId,
        int quantity,
        Instant allocatedAt
) {

    public static ChannelAllocation fromEntity(ChannelAllocationEntity entity) {
        return new ChannelAllocation(
                entity.getId(),
                entity.getProductionRunId(),
                entity.getSalesChannelId(),
                entity.getQuantity(),
                entity.getAllocatedAt()
        );
    }
}
