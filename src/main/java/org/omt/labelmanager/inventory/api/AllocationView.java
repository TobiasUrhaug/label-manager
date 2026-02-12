package org.omt.labelmanager.inventory.api;

import java.time.Instant;

public record AllocationView(
        Long id,
        String channelName,
        int quantity,
        int unitsSold,
        int unitsRemaining,
        Instant allocatedAt
) {
}
