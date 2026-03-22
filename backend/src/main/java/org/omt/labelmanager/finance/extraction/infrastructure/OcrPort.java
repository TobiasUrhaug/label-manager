package org.omt.labelmanager.finance.extraction.infrastructure;

import java.io.InputStream;

/**
 * Port for extracting text from documents using OCR.
 * Implementations may use Tesseract, cloud services, or other OCR engines.
 */
public interface OcrPort {

    /**
     * Extracts text content from a document.
     *
     * @param content the document content stream (PDF or image)
     * @param contentType the MIME type of the document
     * @return the extracted text, or empty string if extraction fails
     */
    String extractText(InputStream content, String contentType);
}
