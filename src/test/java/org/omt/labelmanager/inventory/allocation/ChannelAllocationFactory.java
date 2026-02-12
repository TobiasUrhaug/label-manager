package org.omt.labelmanager.inventory.allocation;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public final class ChannelAllocationFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private ChannelAllocationFactory() {
    }

    public static Builder aChannelAllocation() {
        return new Builder();
    }

    public static ChannelAllocation createDefault() {
        return aChannelAllocation().build();
    }

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private Long productionRunId = 1L;
        private Long distributorId = 1L;
        private int quantity = 100;
        private int unitsSold = 0;
        private Instant allocatedAt = Instant.parse("2025-01-15T10:00:00Z");

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder productionRunId(Long productionRunId) {
            this.productionRunId = productionRunId;
            return this;
        }

        public Builder distributorId(Long distributorId) {
            this.distributorId = distributorId;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder unitsSold(int unitsSold) {
            this.unitsSold = unitsSold;
            return this;
        }

        public Builder allocatedAt(Instant allocatedAt) {
            this.allocatedAt = allocatedAt;
            return this;
        }

        public ChannelAllocation build() {
            return new ChannelAllocation(
                    id,
                    productionRunId,
                    distributorId,
                    quantity,
                    unitsSold,
                    allocatedAt
            );
        }
    }
}
