package org.omt.labelmanager.inventory.allocation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.omt.labelmanager.inventory.allocation.domain.ChannelAllocationFactory.aChannelAllocation;

class ChannelAllocationTest {

    @Test
    void shouldCreateChannelAllocationWithAllFields() {
        Instant allocatedAt = Instant.parse("2025-06-15T14:30:00Z");

        ChannelAllocation allocation = aChannelAllocation()
                .id(42L)
                .productionRunId(10L)
                .distributorId(5L)
                .quantity(200)
                .unitsSold(30)
                .allocatedAt(allocatedAt)
                .build();

        assertThat(allocation.id()).isEqualTo(42L);
        assertThat(allocation.productionRunId()).isEqualTo(10L);
        assertThat(allocation.distributorId()).isEqualTo(5L);
        assertThat(allocation.quantity()).isEqualTo(200);
        assertThat(allocation.unitsSold()).isEqualTo(30);
        assertThat(allocation.allocatedAt()).isEqualTo(allocatedAt);
    }

    @Test
    void shouldCalculateUnitsRemaining() {
        ChannelAllocation allocation = aChannelAllocation()
                .quantity(200)
                .unitsSold(30)
                .build();

        assertThat(allocation.unitsRemaining()).isEqualTo(170);
    }

    @Test
    void shouldCalculateUnitsRemainingWhenNothingSold() {
        ChannelAllocation allocation = aChannelAllocation()
                .quantity(200)
                .unitsSold(0)
                .build();

        assertThat(allocation.unitsRemaining()).isEqualTo(200);
    }
}
