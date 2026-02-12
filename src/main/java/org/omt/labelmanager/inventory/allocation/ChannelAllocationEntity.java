package org.omt.labelmanager.inventory.allocation;

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

    public Instant getAllocatedAt() {
        return allocatedAt;
    }

    public void reduceQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.quantity < amount) {
            throw new IllegalStateException(
                    "Insufficient quantity: available=" + this.quantity
                            + ", requested=" + amount
            );
        }
        this.quantity -= amount;
    }
}
