package org.omt.labelmanager.inventory.allocation.api;

public class AddAllocationForm {

    private Long salesChannelId;
    private int quantity;

    Long getSalesChannelId() {
        return salesChannelId;
    }

    void setSalesChannelId(Long salesChannelId) {
        this.salesChannelId = salesChannelId;
    }

    int getQuantity() {
        return quantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
