package org.omt.labelmanager.inventory.inventorymovement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_run_id", nullable = false)
    private Long productionRunId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_location_type", nullable = false)
    private LocationType fromLocationType;

    @Column(name = "from_location_id")
    private Long fromLocationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_location_type", nullable = false)
    private LocationType toLocationType;

    @Column(name = "to_location_id")
    private Long toLocationId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

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
            LocationType fromLocationType,
            Long fromLocationId,
            LocationType toLocationType,
            Long toLocationId,
            int quantity,
            MovementType movementType,
            Instant occurredAt,
            Long referenceId
    ) {
        this.productionRunId = productionRunId;
        this.fromLocationType = fromLocationType;
        this.fromLocationId = fromLocationId;
        this.toLocationType = toLocationType;
        this.toLocationId = toLocationId;
        this.quantity = quantity;
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

    public LocationType getFromLocationType() {
        return fromLocationType;
    }

    public Long getFromLocationId() {
        return fromLocationId;
    }

    public LocationType getToLocationType() {
        return toLocationType;
    }

    public Long getToLocationId() {
        return toLocationId;
    }

    public int getQuantity() {
        return quantity;
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
