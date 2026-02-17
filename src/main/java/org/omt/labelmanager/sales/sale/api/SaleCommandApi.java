package org.omt.labelmanager.sales.sale.api;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;

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
     * @param distributorId the distributor (required for non-DIRECT channels, null
     *        for DIRECT)
     * @param lineItems the items sold
     * @return the created sale
     */
    Sale registerSale(
            Long labelId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            Long distributorId,
            List<SaleLineItemInput> lineItems
    );

    /**
     * Update an existing sale. Replaces all line items and adjusts inventory movements
     * accordingly. Old movements are reversed before new inventory is validated,
     * so the full allocated quantity is available for re-validation.
     *
     * @param saleId the ID of the sale to update
     * @param saleDate the new sale date
     * @param notes updated notes (may be null)
     * @param lineItems the new line items (must not be empty)
     * @return the updated sale
     */
    Sale updateSale(
            Long saleId,
            LocalDate saleDate,
            String notes,
            List<SaleLineItemInput> lineItems
    );

    /**
     * Delete a sale and reverse all its inventory movements.
     *
     * @param saleId the ID of the sale to delete
     */
    void deleteSale(Long saleId);
}
