package org.omt.labelmanager.inventory.allocation.domain;

public class InsufficientInventoryException extends RuntimeException {

    private final int requested;
    private final int available;

    public InsufficientInventoryException(int requested, int available) {
        super(String.format(
                "Insufficient inventory: requested %d but only %d available",
                requested,
                available
        ));
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
