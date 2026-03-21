package org.omt.labelmanager.distribution.agreement.api;

import jakarta.validation.constraints.NotNull;
import org.omt.labelmanager.distribution.agreement.CommissionType;

import java.math.BigDecimal;

public class AgreementForm {

    private Long productionRunId;
    @NotNull
    private BigDecimal unitPrice;
    @NotNull
    private CommissionType commissionType;
    @NotNull
    private BigDecimal commissionValue;

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
}
