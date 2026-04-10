package org.omt.labelmanager.finance.extraction.infrastructure;

import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class ExternalInvoiceParserAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExternalInvoiceParserAdapter.class);

    private final RestClient restClient;

    ExternalInvoiceParserAdapter(@Qualifier("invoiceParserRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ExtractedInvoiceData extract(InputStream content, String contentType) {
        try {
            var response = postToExternalParser(content, contentType);
            if (response == null) {
                log.warn("External invoice parser returned empty body");
                return ExtractedInvoiceData.empty();
            }
            return mapToExtractedInvoiceData(response);
        } catch (HttpStatusCodeException e) {
            logHttpError(e);
            return ExtractedInvoiceData.empty();
        } catch (Exception e) {
            log.warn("External invoice parser failed: {}", e.getMessage());
            return ExtractedInvoiceData.empty();
        }
    }

    private ExternalInvoiceResponse postToExternalParser(InputStream content, String contentType) {
        var fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        var filePart = new HttpEntity<>(new InputStreamResource(content), fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);

        return restClient.post()
                .uri("/api/v1/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(ExternalInvoiceResponse.class);
    }

    private void logHttpError(HttpStatusCodeException e) {
        var requestId = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst("X-Request-Id")
                : null;
        if (requestId != null) {
            log.warn("External invoice parser failed with status {}, X-Request-Id: {}", e.getStatusCode(), requestId);
        } else {
            log.warn("External invoice parser failed with status {}", e.getStatusCode());
        }
    }

    private ExtractedInvoiceData mapToExtractedInvoiceData(ExternalInvoiceResponse response) {
        return new ExtractedInvoiceData(
                parseAmount(response.netAmount()),
                parseAmount(response.vatAmount()),
                null,
                parseAmount(response.totalAmount()),
                parseDate(response.invoiceDate()),
                response.invoiceReference(),
                parseCurrency(response.netAmount())
        );
    }

    private BigDecimal parseAmount(ExternalInvoiceResponse.MoneyAmount moneyAmount) {
        if (moneyAmount == null || moneyAmount.amount() == null) {
            return null;
        }
        return new BigDecimal(moneyAmount.amount());
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        return LocalDate.parse(dateString);
    }

    private String parseCurrency(ExternalInvoiceResponse.MoneyAmount moneyAmount) {
        return moneyAmount != null ? moneyAmount.currency() : null;
    }
}
