package org.omt.labelmanager.finance.extraction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data extracted from an invoice document.
 * All fields are nullable - extraction may succeed partially.
 */
public record ExtractedInvoiceData(
        BigDecimal netAmount,
        BigDecimal vatAmount,
        BigDecimal vatRate,
        BigDecimal grossAmount,
        LocalDate invoiceDate,
        String invoiceReference,
        String currency
) {

    /**
     * Returns an empty result when extraction fails completely.
     */
    public static ExtractedInvoiceData empty() {
        return new ExtractedInvoiceData(null, null, null, null, null, null, null);
    }

    /**
     * Returns true if at least one field was extracted.
     */
    public boolean hasAnyData() {
        return netAmount != null
                || vatAmount != null
                || vatRate != null
                || grossAmount != null
                || invoiceDate != null
                || invoiceReference != null
                || currency != null;
    }
}
