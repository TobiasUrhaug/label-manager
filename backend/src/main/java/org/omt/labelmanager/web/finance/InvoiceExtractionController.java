package org.omt.labelmanager.web.finance;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.omt.labelmanager.finance.extraction.api.ExtractionCommandApi;
import org.omt.labelmanager.finance.extraction.api.InvalidDocumentTypeException;
import org.omt.labelmanager.finance.extraction.api.InvoiceParserUnavailableException;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/costs")
public class InvoiceExtractionController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExtractionController.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf");

    private final ExtractionCommandApi extractionCommandApi;

    public InvoiceExtractionController(ExtractionCommandApi extractionCommandApi) {
        this.extractionCommandApi = extractionCommandApi;
    }

    /**
     * The extraction result, flattened so a client reads one object.
     *
     * <p>{@code extracted} is false when the parser ran and found nothing. Every other field is
     * then null. A parser that could not be reached does not produce this at all — that is a 502.
     */
    record ExtractionResponse(
            boolean extracted,
            BigDecimal netAmount,
            BigDecimal vatAmount,
            BigDecimal vatRate,
            BigDecimal grossAmount,
            LocalDate invoiceDate,
            String invoiceReference,
            String currency) {

        static ExtractionResponse of(ExtractedInvoiceData data) {
            return new ExtractionResponse(
                    data.hasAnyData(),
                    data.netAmount(),
                    data.vatAmount(),
                    data.vatRate(),
                    data.grossAmount(),
                    data.invoiceDate(),
                    data.invoiceReference(),
                    data.currency());
        }
    }

    @PostMapping("/extract")
    public ExtractionResponse extractInvoiceData(@RequestParam("document") MultipartFile document)
            throws IOException {
        if (document.isEmpty()) {
            throw new IllegalArgumentException("No document was provided for extraction");
        }

        String contentType = document.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("Unsupported document type for extraction: {}", contentType);
            throw new InvalidDocumentTypeException(contentType);
        }

        log.info("Extracting invoice data from document: {}", document.getOriginalFilename());

        ExtractedInvoiceData result =
                extractionCommandApi.extract(document.getInputStream(), contentType);

        return ExtractionResponse.of(result);
    }

    @ExceptionHandler(InvoiceParserUnavailableException.class)
    public ProblemDetail handleParserUnavailable(InvoiceParserUnavailableException exception) {
        log.error("Invoice extraction failed", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }
}
