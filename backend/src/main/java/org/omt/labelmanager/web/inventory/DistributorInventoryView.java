package org.omt.labelmanager.web.inventory;

/**
 * Per-distributor inventory summary for a single production run, showing current stock derived from
 * inventory movements.
 */
public record DistributorInventoryView(String name, int current) {}
