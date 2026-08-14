package org.omt.labelmanager.web.inventory;

/**
 * How much of a production run one distributor currently holds, derived from inventory movements.
 *
 * <p>Identified by id rather than name, so inventory does not read from distribution to render a
 * label.
 */
public record DistributorInventoryView(Long distributorId, int current) {}
