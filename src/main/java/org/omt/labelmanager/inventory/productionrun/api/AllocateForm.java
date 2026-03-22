package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.inventory.LocationType;

public class AllocateForm {

    private LocationType locationType;
    private Long distributorId;
    private int quantity;

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

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
