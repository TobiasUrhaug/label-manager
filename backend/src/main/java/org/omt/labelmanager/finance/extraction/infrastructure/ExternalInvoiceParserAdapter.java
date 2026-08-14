package org.omt.labelmanager.finance.extraction.infrastructure;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.omt.labelmanager.finance.extraction.api.InvoiceParserUnavailableException;
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
import org.springframework.web.client.RestClientException;

@Component
public class ExternalInvoiceParserAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExternalInvoiceParserAdapter.class);

    private final RestClient restClient;

    ExternalInvoiceParserAdapter(@Qualifier("invoiceParserRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ExtractedInvoiceData extract(InputStream content, String contentType) {
        ExternalInvoiceResponse response;
        try {
            response = postToExternalParser(content, contentType);
        } catch (HttpStatusCodeException e) {
            logHttpError(e);
            throw new InvoiceParserUnavailableException(
                    "Invoice parser answered with status " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            log.warn("External invoice parser could not be called: {}", e.getMessage());
            throw new InvoiceParserUnavailableException("Invoice parser could not be reached", e);
        }

        if (response == null) {
            // A 2xx with no body is a parse that found nothing, not a broken integration.
            log.warn("External invoice parser returned empty body");
            return ExtractedInvoiceData.empty();
        }

        try {
            return mapToExtractedInvoiceData(response);
        } catch (RuntimeException e) {
            // The parser answered, so it is not down — but an unparseable amount or date means
            // its response does not match the contract. Still a 502, because the upstream reply
            // is the problem; the message must not claim it was unreachable.
            log.warn("External invoice parser returned an unreadable response", e);
            throw new InvoiceParserUnavailableException(
                    "Invoice parser returned a response that could not be read", e);
        }
    }

    private ExternalInvoiceResponse postToExternalParser(InputStream content, String contentType) {
        var fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        var filePart = new HttpEntity<>(new InputStreamResource(content), fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);

        return restClient
                .post()
                .uri("/api/v1/extract")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(ExternalInvoiceResponse.class);
    }

    private void logHttpError(HttpStatusCodeException e) {
        var requestId =
                e.getResponseHeaders() != null
                        ? e.getResponseHeaders().getFirst("X-Request-Id")
                        : null;
        if (requestId != null) {
            log.warn(
                    "External invoice parser failed with status {}, X-Request-Id: {}",
                    e.getStatusCode(),
                    requestId);
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
                parseCurrency(response.netAmount()));
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
