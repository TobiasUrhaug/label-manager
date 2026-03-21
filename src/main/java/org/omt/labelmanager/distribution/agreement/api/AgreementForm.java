package org.omt.labelmanager.distribution.agreement.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AgreementForm {

    private Long productionRunId;
    @NotNull
    private BigDecimal unitPrice;
    @NotNull
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
