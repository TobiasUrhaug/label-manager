package org.omt.labelmanager.inventory;

/**
 * Thrown when an operation requires more inventory than is currently available.
 *
 * <p>Used by allocation and sale operations throughout the inventory bounded context.
 */
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
