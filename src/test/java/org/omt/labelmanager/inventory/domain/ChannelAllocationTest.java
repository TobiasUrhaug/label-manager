package org.omt.labelmanager.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.omt.labelmanager.inventory.domain.ChannelAllocationFactory.aChannelAllocation;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChannelAllocationTest {

    @Test
    void shouldCreateChannelAllocationWithAllFields() {
        Instant allocatedAt = Instant.parse("2025-06-15T14:30:00Z");

        ChannelAllocation allocation = aChannelAllocation()
                .id(42L)
                .productionRunId(10L)
                .salesChannelId(5L)
                .quantity(200)
                .allocatedAt(allocatedAt)
                .build();

        assertThat(allocation.id()).isEqualTo(42L);
        assertThat(allocation.productionRunId()).isEqualTo(10L);
        assertThat(allocation.salesChannelId()).isEqualTo(5L);
        assertThat(allocation.quantity()).isEqualTo(200);
        assertThat(allocation.allocatedAt()).isEqualTo(allocatedAt);
    }
}
