package org.omt.labelmanager.sales.sale.api;

import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.sales.sale.domain.Sale;

import java.util.List;
import java.util.Optional;

/**
 * Public API for sale query operations.
 */
public interface SaleQueryApi {

    /**
     * Get all sales for a label, ordered by date (newest first).
     *
     * @param labelId the label ID
     * @return list of sales
     */
    List<Sale> getSalesForLabel(Long labelId);

    /**
     * Find a sale by ID.
     *
     * @param saleId the sale ID
     * @return the sale, if found
     */
    Optional<Sale> findById(Long saleId);

    /**
     * Calculate total revenue for a label across all sales.
     *
     * @param labelId the label ID
     * @return total revenue
     */
    Money getTotalRevenueForLabel(Long labelId);
}
