package org.omt.labelmanager.inventory.allocation.api;

public class AddAllocationForm {

    private Long distributorId;
    private int quantity;

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
