package org.omt.labelmanager.sales.sale.api;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;

import java.time.LocalDate;
import java.util.List;

/**
 * Public API for sale command operations.
 */
public interface SaleCommandApi {

    /**
     * Register a new sale.
     *
     * @param labelId the label making the sale
     * @param saleDate the date of the sale
     * @param channel the sales channel (EVENT, DIRECT, etc.)
     * @param notes optional notes about the sale
     * @param lineItems the items sold
     * @return the created sale
     */
    Sale registerSale(
            Long labelId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            List<SaleLineItemInput> lineItems
    );
}
