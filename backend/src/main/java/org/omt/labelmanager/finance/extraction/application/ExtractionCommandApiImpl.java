package org.omt.labelmanager.finance.extraction.application;

import java.io.InputStream;
import org.omt.labelmanager.finance.extraction.api.ExtractionCommandApi;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.springframework.stereotype.Service;

/**
 * Implementation of the extraction command API.
 * Delegates to the extraction use case.
 */
@Service
class ExtractionCommandApiImpl implements ExtractionCommandApi {

    private final ExtractInvoiceDataUseCase extractInvoiceData;

    public ExtractionCommandApiImpl(ExtractInvoiceDataUseCase extractInvoiceData) {
        this.extractInvoiceData = extractInvoiceData;
    }

    @Override
    public ExtractedInvoiceData extract(InputStream content, String contentType) {
        return extractInvoiceData.extract(content, contentType);
    }
}
