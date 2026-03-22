package org.omt.labelmanager.sales.sale.domain;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.finance.domain.shared.Money;

/**
 * A line item in a sale representing a release/format sold.
 */
public record SaleLineItem(
        Long id,
        Long releaseId,
        ReleaseFormat format,
        int quantity,
        Money unitPrice,
        Money lineTotal
) {
}
