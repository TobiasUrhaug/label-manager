package org.omt.labelmanager.finance.extraction.api;

import java.io.IOException;
import java.util.Set;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/costs")
public class InvoiceExtractionController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceExtractionController.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    private final ExtractionCommandApi extractionCommandApi;

    public InvoiceExtractionController(ExtractionCommandApi extractionCommandApi) {
        this.extractionCommandApi = extractionCommandApi;
    }

    @PostMapping("/extract")
    public ResponseEntity<ExtractedInvoiceData> extractInvoiceData(
            @RequestParam("document") MultipartFile document
    ) throws IOException {
        if (document == null || document.isEmpty()) {
            log.warn("No document provided for extraction");
            return ResponseEntity.badRequest().build();
        }

        String contentType = document.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("Unsupported document type for extraction: {}", contentType);
            throw new InvalidDocumentTypeException(contentType);
        }

        log.info("Extracting invoice data from document: {}", document.getOriginalFilename());

        ExtractedInvoiceData result = extractionCommandApi.extract(
                document.getInputStream(),
                contentType
        );

        return ResponseEntity.ok(result);
    }
}
