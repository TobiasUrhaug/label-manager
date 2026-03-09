package org.omt.labelmanager.sales.distributor_return.domain;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;

/**
 * Value object carrying the data for a single return line item submitted by the user.
 */
public record ReturnLineItemInput(
        Long releaseId,
        ReleaseFormat format,
        int quantity
) {
}
