package org.omt.labelmanager.finance.extraction.application;

import java.io.InputStream;
import org.omt.labelmanager.finance.extraction.api.ExtractionCommandApi;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.omt.labelmanager.finance.extraction.infrastructure.ExternalInvoiceParserAdapter;
import org.springframework.stereotype.Service;

@Service
class ExtractionCommandApiImpl implements ExtractionCommandApi {

    private final ExternalInvoiceParserAdapter externalInvoiceParser;

    ExtractionCommandApiImpl(ExternalInvoiceParserAdapter externalInvoiceParser) {
        this.externalInvoiceParser = externalInvoiceParser;
    }

    @Override
    public ExtractedInvoiceData extract(InputStream content, String contentType) {
        return externalInvoiceParser.extract(content, contentType);
    }
}
