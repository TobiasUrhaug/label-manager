package org.omt.labelmanager.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "channel_allocation")
public class ChannelAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "production_run_id", nullable = false)
    private Long productionRunId;

    @Column(name = "sales_channel_id", nullable = false)
    private Long salesChannelId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "allocated_at", nullable = false)
    private Instant allocatedAt;

    protected ChannelAllocationEntity() {
    }

    public ChannelAllocationEntity(
            Long productionRunId,
            Long salesChannelId,
            int quantity,
            Instant allocatedAt
    ) {
        this.productionRunId = productionRunId;
        this.salesChannelId = salesChannelId;
        this.quantity = quantity;
        this.allocatedAt = allocatedAt;
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

    public int getQuantity() {
        return quantity;
    }

    public Instant getAllocatedAt() {
        return allocatedAt;
    }
}
