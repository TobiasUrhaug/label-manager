package org.omt.labelmanager.sales.sale.domain;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.omt.labelmanager.finance.domain.shared.Money;

/** Represents a sale transaction attributed to a specific distributor. */
public record Sale(
        Long id,
        Long labelId,
        Long distributorId,
        LocalDate saleDate,
        ChannelType channel,
        String notes,
        List<SaleLineItem> lineItems,
        Money totalAmount) {}
