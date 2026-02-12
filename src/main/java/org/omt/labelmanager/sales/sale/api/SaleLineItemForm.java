package org.omt.labelmanager.sales.sale.api;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;

import java.math.BigDecimal;

/**
 * Form backing bean for a single sale line item.
 */
public class SaleLineItemForm {

    private Long releaseId;
    private ReleaseFormat format;
    private int quantity;
    private BigDecimal unitPrice;

    public SaleLineItemForm() {
    }

    public SaleLineItemForm(Long releaseId, ReleaseFormat format, int quantity, BigDecimal unitPrice) {
        this.releaseId = releaseId;
        this.format = format;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public SaleLineItemInput toInput() {
        return new SaleLineItemInput(
                releaseId,
                format,
                quantity,
                Money.of(unitPrice)
        );
    }

    // Getters and setters

    public Long getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(Long releaseId) {
        this.releaseId = releaseId;
    }

    public ReleaseFormat getFormat() {
        return format;
    }

    public void setFormat(ReleaseFormat format) {
        this.format = format;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
