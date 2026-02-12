package org.omt.labelmanager.sales.sale.domain;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.finance.domain.shared.Money;

/**
 * Input DTO for creating a sale line item.
 */
public record SaleLineItemInput(
        Long releaseId,
        ReleaseFormat format,
        int quantity,
        Money unitPrice
) {
}
