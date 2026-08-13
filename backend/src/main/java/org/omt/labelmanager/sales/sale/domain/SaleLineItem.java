package org.omt.labelmanager.sales.sale.domain;

import org.omt.labelmanager.shared.Format;
import org.omt.labelmanager.shared.Money;

/** A line item in a sale representing a release/format sold. */
public record SaleLineItem(
        Long id, Long releaseId, Format format, int quantity, Money unitPrice, Money lineTotal) {}
