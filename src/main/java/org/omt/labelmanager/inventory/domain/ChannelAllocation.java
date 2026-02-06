package org.omt.labelmanager.inventory.domain;

import java.time.Instant;
import org.omt.labelmanager.inventory.infrastructure.persistence.ChannelAllocationEntity;

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
