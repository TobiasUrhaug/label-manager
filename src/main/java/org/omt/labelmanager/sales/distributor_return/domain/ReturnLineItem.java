package org.omt.labelmanager.sales.distributor_return.domain;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;

/**
 * A single line item within a distributor return, representing one release format
 * and quantity being returned from the distributor to the warehouse.
 */
public record ReturnLineItem(
        Long id,
        Long returnId,
        Long releaseId,
        ReleaseFormat format,
        int quantity
) {
}
