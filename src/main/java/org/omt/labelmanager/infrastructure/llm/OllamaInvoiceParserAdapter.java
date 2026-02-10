package org.omt.labelmanager.infrastructure.llm;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.omt.labelmanager.finance.application.extraction.InvoiceParserPort;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OllamaInvoiceParserAdapter implements InvoiceParserPort {

    private static final Logger log = LoggerFactory.getLogger(OllamaInvoiceParserAdapter.class);

    private static final String EXTRACTION_PROMPT = """
            Extract invoice data from the following text. \
            Return ONLY a JSON object with these fields:
            - netAmount: number (amount before tax)
            - vatAmount: number (tax amount)
            - vatRate: number (tax rate as percentage, e.g., 21 for 21 percent)
            - grossAmount: number (total amount including tax)
            - invoiceDate: string (date in YYYY-MM-DD format)
            - invoiceReference: string (invoice number/reference)
            - currency: string (3-letter currency code like EUR, USD, GBP)

            If a field cannot be found, use null. Return only the JSON, no other text.

            Invoice text:
            %s
            """;

    private final RestClient restClient;
    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaInvoiceParserAdapter(
            RestClient.Builder restClientBuilder,
            OllamaProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExtractedInvoiceData parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            log.warn("Cannot parse empty or null text");
            return ExtractedInvoiceData.empty();
        }

        log.info("Parsing invoice text with Ollama (model: {})", properties.model());
        log.debug("Invoice text length: {} characters", rawText.length());
        log.debug("Raw OCR text being sent to LLM:\n{}", rawText);

        try {
            String response = callOllama(rawText);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("Failed to parse invoice: {}", e.getMessage());
            return ExtractedInvoiceData.empty();
        }
    }

    private String callOllama(String text) {
        String prompt = String.format(EXTRACTION_PROMPT, text);

        Map<String, Object> request = Map.of(
                "model", properties.model(),
                "prompt", prompt,
                "stream", false
        );

        log.debug("Calling Ollama API at {}", properties.baseUrl());

        OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null || response.response() == null) {
            throw new RuntimeException("Empty response from Ollama");
        }

        log.debug("Received response from Ollama: {} characters", response.response().length());
        log.debug("Full LLM response:\n{}", response.response());
        return response.response();
    }

    private ExtractedInvoiceData parseResponse(String response) throws Exception {
        String json = extractJsonFromResponse(response);
        log.debug("Extracted JSON: {}", json);

        ParsedInvoice parsed = objectMapper.readValue(json, ParsedInvoice.class);

        return new ExtractedInvoiceData(
                parseBigDecimal(parsed.netAmount),
                parseBigDecimal(parsed.vatAmount),
                parseBigDecimal(parsed.vatRate),
                parseBigDecimal(parsed.grossAmount),
                parseDate(parsed.invoiceDate),
                parsed.invoiceReference,
                parsed.currency != null ? parsed.currency : properties.defaultCurrency()
        );
    }

    private String extractJsonFromResponse(String response) {
        String trimmed = response.trim();

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            log.debug("Could not parse number: {}", value);
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.debug("Could not parse date: {}", value);
            return null;
        }
    }

    record OllamaResponse(String response) {}

    record ParsedInvoice(
            Object netAmount,
            Object vatAmount,
            Object vatRate,
            Object grossAmount,
            String invoiceDate,
            String invoiceReference,
            String currency
    ) {}
}
