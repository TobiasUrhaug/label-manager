package org.omt.labelmanager.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.omt.labelmanager.inventory.domain.MovementType;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_run_id", nullable = false)
    private Long productionRunId;

    @Column(name = "sales_channel_id", nullable = false)
    private Long salesChannelId;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false)
    private MovementType movementType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reference_id")
    private Long referenceId;

    protected InventoryMovementEntity() {
    }

    public InventoryMovementEntity(
            Long productionRunId,
            Long salesChannelId,
            int quantityDelta,
            MovementType movementType,
            Instant occurredAt,
            Long referenceId
    ) {
        this.productionRunId = productionRunId;
        this.salesChannelId = salesChannelId;
        this.quantityDelta = quantityDelta;
        this.movementType = movementType;
        this.occurredAt = occurredAt;
        this.referenceId = referenceId;
    }

    public Long getId() {
        return id;
    }

    public Long getProductionRunId() {
        return productionRunId;
    }

    public Long getSalesChannelId() {
        return salesChannelId;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Long getReferenceId() {
        return referenceId;
    }
}
