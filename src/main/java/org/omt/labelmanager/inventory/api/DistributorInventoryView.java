package org.omt.labelmanager.inventory.api;

/**
 * Per-distributor inventory summary for a single production run.
 *
 * <p>Combines allocation history with current movement-based inventory to give
 * a complete picture of what was allocated, how much remains, and how much has
 * been sold.
 */
public record DistributorInventoryView(
        String name,
        int allocated,
        int current
) {

    /** Units sold = originally allocated minus units still held by the distributor. */
    public int sold() {
        return allocated - current;
    }
}
