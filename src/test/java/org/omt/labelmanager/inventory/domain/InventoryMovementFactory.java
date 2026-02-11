package org.omt.labelmanager.inventory.domain;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public final class InventoryMovementFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private InventoryMovementFactory() {
    }

    public static Builder anInventoryMovement() {
        return new Builder();
    }

    public static InventoryMovement createDefault() {
        return anInventoryMovement().build();
    }

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private Long productionRunId = 1L;
        private Long distributorId = 1L;
        private int quantityDelta = 100;
        private MovementType movementType = MovementType.ALLOCATION;
        private Instant occurredAt = Instant.parse("2025-01-15T10:00:00Z");
        private Long referenceId = null;

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

        public Builder quantityDelta(int quantityDelta) {
            this.quantityDelta = quantityDelta;
            return this;
        }

        public Builder movementType(MovementType movementType) {
            this.movementType = movementType;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder referenceId(Long referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public InventoryMovement build() {
            return new InventoryMovement(
                    id,
                    productionRunId,
                    distributorId,
                    quantityDelta,
                    movementType,
                    occurredAt,
                    referenceId
            );
        }
    }
}
