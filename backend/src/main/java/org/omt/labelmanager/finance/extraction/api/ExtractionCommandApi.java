package org.omt.labelmanager.finance.extraction.api;

import java.io.InputStream;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;

/** Command API for invoice data extraction operations. */
public interface ExtractionCommandApi {

    /**
     * Extracts invoice data from a document.
     *
     * @param content the document content stream
     * @param contentType the MIME type of the document
     * @return the extracted data, or {@link ExtractedInvoiceData#empty()} when the parser ran and
     *     found nothing
     * @throws InvoiceParserUnavailableException if the parser could not be reached, answered with
     *     an error status, or returned something that could not be read
     */
    ExtractedInvoiceData extract(InputStream content, String contentType);
}
