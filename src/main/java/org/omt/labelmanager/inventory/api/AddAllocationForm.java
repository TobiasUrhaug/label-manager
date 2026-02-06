package org.omt.labelmanager.inventory.api;

public class AddAllocationForm {

    private Long salesChannelId;
    private int quantity;

    public Long getSalesChannelId() {
        return salesChannelId;
    }

    public void setSalesChannelId(Long salesChannelId) {
        this.salesChannelId = salesChannelId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
