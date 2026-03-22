package org.omt.labelmanager.finance.extraction.infrastructure;

import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;

/**
 * Port for parsing invoice text and extracting structured field data.
 * Implementations may use LLMs, regex patterns, or other parsing strategies.
 */
public interface InvoiceParserPort {

    /**
     * Parses raw text from an invoice and extracts structured data.
     *
     * @param rawText the OCR-extracted text from an invoice document
     * @return extracted invoice data with any fields that could be identified
     */
    ExtractedInvoiceData parse(String rawText);
}
