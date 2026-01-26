package org.omt.labelmanager.finance.cost.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.omt.labelmanager.finance.shared.Money;
import org.omt.labelmanager.finance.cost.CostType;
import org.omt.labelmanager.finance.cost.VatAmount;

public class RegisterCostForm {

    private BigDecimal netAmount;
    private BigDecimal vatAmount;
    private BigDecimal vatRate;
    private BigDecimal grossAmount;
    private CostType costType;
    private LocalDate incurredOn;
    private String description;
    private String documentReference;

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public CostType getCostType() {
        return costType;
    }

    public void setCostType(CostType costType) {
        this.costType = costType;
    }

    public LocalDate getIncurredOn() {
        return incurredOn;
    }

    public void setIncurredOn(LocalDate incurredOn) {
        this.incurredOn = incurredOn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocumentReference() {
        return documentReference;
    }

    public void setDocumentReference(String documentReference) {
        this.documentReference = documentReference;
    }

    public Money toNetAmount() {
        return Money.of(netAmount);
    }

    public VatAmount toVatAmount() {
        return new VatAmount(Money.of(vatAmount), vatRate);
    }

    public Money toGrossAmount() {
        return Money.of(grossAmount);
    }
}
