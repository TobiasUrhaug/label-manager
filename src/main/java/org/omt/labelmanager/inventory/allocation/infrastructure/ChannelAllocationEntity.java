package org.omt.labelmanager.inventory.allocation.infrastructure;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "channel_allocation")
public class ChannelAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_run_id", nullable = false)
    private Long productionRunId;

    @Column(name = "distributor_id", nullable = false)
    private Long distributorId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "units_sold", nullable = false)
    private int unitsSold;

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt;

    protected ChannelAllocationEntity() {
    }

    public ChannelAllocationEntity(
            Long productionRunId,
            Long distributorId,
            int quantity,
            Instant allocatedAt
    ) {
        this.productionRunId = productionRunId;
        this.distributorId = distributorId;
        this.quantity = quantity;
        this.unitsSold = 0;
        this.allocatedAt = allocatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductionRunId() {
        return productionRunId;
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitsSold() {
        return unitsSold;
    }

    public Instant getAllocatedAt() {
        return allocatedAt;
    }

    public void incrementUnitsSold(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int unitsRemaining = this.quantity - this.unitsSold;
        if (unitsRemaining < amount) {
            throw new IllegalStateException(
                    "Insufficient quantity: available=" + unitsRemaining
                            + ", requested=" + amount
            );
        }
        this.unitsSold += amount;
    }
}
