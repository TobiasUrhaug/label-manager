package org.omt.labelmanager.distribution.agreement.infrastructure;

import jakarta.persistence.*;
import org.omt.labelmanager.distribution.agreement.domain.CommissionType;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false)
    private CommissionType commissionType;

    @Column(name = "commission_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PricingAgreementEntity() {
    }

    public PricingAgreementEntity(
            Long distributorId,
            Long productionRunId,
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue
    ) {
        this.distributorId = distributorId;
        this.productionRunId = productionRunId;
        this.unitPrice = unitPrice;
        this.commissionType = commissionType;
        this.commissionValue = commissionValue;
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

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public void setCommissionType(CommissionType commissionType) {
        this.commissionType = commissionType;
    }

    public BigDecimal getCommissionValue() {
        return commissionValue;
    }

    public void setCommissionValue(BigDecimal commissionValue) {
        this.commissionValue = commissionValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
