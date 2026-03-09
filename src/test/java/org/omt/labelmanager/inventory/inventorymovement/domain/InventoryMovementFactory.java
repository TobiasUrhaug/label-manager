package org.omt.labelmanager.inventory.inventorymovement.domain;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;

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
        private LocationType fromLocationType = LocationType.WAREHOUSE;
        private Long fromLocationId = null;
        private LocationType toLocationType = LocationType.DISTRIBUTOR;
        private Long toLocationId = 1L;
        private int quantity = 100;
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

        public Builder fromLocationType(LocationType fromLocationType) {
            this.fromLocationType = fromLocationType;
            return this;
        }

        public Builder fromLocationId(Long fromLocationId) {
            this.fromLocationId = fromLocationId;
            return this;
        }

        public Builder toLocationType(LocationType toLocationType) {
            this.toLocationType = toLocationType;
            return this;
        }

        public Builder toLocationId(Long toLocationId) {
            this.toLocationId = toLocationId;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
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
                    fromLocationType,
                    fromLocationId,
                    toLocationType,
                    toLocationId,
                    quantity,
                    movementType,
                    occurredAt,
                    referenceId
            );
        }
    }
}
