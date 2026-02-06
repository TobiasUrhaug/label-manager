package org.omt.labelmanager.inventory.domain;

import java.time.Instant;

public record ChannelAllocation(
        Long id,
        Long productionRunId,
        Long salesChannelId,
        int quantity,
        Instant allocatedAt
) {
}
