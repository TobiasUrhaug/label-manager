package org.omt.labelmanager.sales.sale.domain;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.finance.domain.shared.Money;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a sale transaction.
 */
public record Sale(
        Long id,
        Long labelId,
        LocalDate saleDate,
        ChannelType channel,
        String notes,
        List<SaleLineItem> lineItems,
        Money totalAmount
) {
}
