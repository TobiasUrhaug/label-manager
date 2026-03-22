package org.omt.labelmanager.sales.distributor_return.api;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;

/**
 * Form backing bean for a single return line item.
 */
public class ReturnLineItemForm {

    private Long releaseId;
    private ReleaseFormat format;
    private int quantity;

    public ReturnLineItemForm() {
    }

    public ReturnLineItemForm(Long releaseId, ReleaseFormat format, int quantity) {
        this.releaseId = releaseId;
        this.format = format;
        this.quantity = quantity;
    }

    public ReturnLineItemInput toInput() {
        return new ReturnLineItemInput(releaseId, format, quantity);
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
}
