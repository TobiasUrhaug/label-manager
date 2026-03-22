package org.omt.labelmanager.inventory.api;

/**
 * Per-distributor inventory summary for a single production run,
 * showing current stock derived from inventory movements.
 */
public record DistributorInventoryView(
        String name,
        int current
) {
}
