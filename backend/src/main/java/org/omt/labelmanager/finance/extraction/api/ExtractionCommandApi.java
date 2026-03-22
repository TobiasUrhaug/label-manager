package org.omt.labelmanager.finance.extraction.api;

import java.io.InputStream;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;

/**
 * Command API for invoice data extraction operations.
 */
public interface ExtractionCommandApi {

    /**
     * Extracts invoice data from a document.
     *
     * @param content the document content stream
     * @param contentType the MIME type of the document
     * @return extracted invoice data, or empty data if extraction fails
     */
    ExtractedInvoiceData extract(InputStream content, String contentType);
}
