package org.omt.labelmanager.distribution.agreement.infrastructure;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pricing_agreement")
public class PricingAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "distributor_id", nullable = false)
    private Long distributorId;

    @Column(name = "production_run_id", nullable = false)
    private Long productionRunId;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PricingAgreementEntity() {
    }

    public PricingAgreementEntity(
            Long distributorId,
            Long productionRunId,
            BigDecimal unitPrice,
            BigDecimal commissionPercentage
    ) {
        this.distributorId = distributorId;
        this.productionRunId = productionRunId;
        this.unitPrice = unitPrice;
        this.commissionPercentage = commissionPercentage;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public Long getProductionRunId() {
        return productionRunId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
