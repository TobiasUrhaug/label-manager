package org.omt.labelmanager.finance.extraction.application;

import java.io.InputStream;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.omt.labelmanager.finance.extraction.infrastructure.InvoiceParserPort;
import org.omt.labelmanager.finance.extraction.infrastructure.OcrPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Use case for extracting invoice data from uploaded documents.
 * Orchestrates OCR text extraction followed by LLM-based field parsing.
 */
@Service
public class ExtractInvoiceDataUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExtractInvoiceDataUseCase.class);

    private final OcrPort ocrPort;
    private final InvoiceParserPort invoiceParserPort;

    public ExtractInvoiceDataUseCase(OcrPort ocrPort, InvoiceParserPort invoiceParserPort) {
        this.ocrPort = ocrPort;
        this.invoiceParserPort = invoiceParserPort;
    }

    /**
     * Extracts invoice data from a document.
     *
     * @param content the document content stream
     * @param contentType the MIME type of the document
     * @return extracted invoice data, or empty data if extraction fails
     */
    public ExtractedInvoiceData extract(InputStream content, String contentType) {
        log.info("Starting invoice data extraction for content type '{}'", contentType);

        String rawText = ocrPort.extractText(content, contentType);

        if (rawText == null || rawText.isBlank()) {
            log.warn("OCR extracted no text from document");
            return ExtractedInvoiceData.empty();
        }

        log.debug("OCR extracted {} characters", rawText.length());

        ExtractedInvoiceData result = invoiceParserPort.parse(rawText);

        if (result.hasAnyData()) {
            log.info("Successfully extracted invoice data");
        } else {
            log.warn("Could not extract any fields from invoice");
        }

        return result;
    }
}
