package org.omt.labelmanager.distribution.agreement.api;

import java.math.BigDecimal;

public class AgreementForm {

    private Long productionRunId;
    private BigDecimal unitPrice;
    private BigDecimal commissionPercentage;

    public Long getProductionRunId() {
        return productionRunId;
    }

    public void setProductionRunId(Long productionRunId) {
        this.productionRunId = productionRunId;
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
}
