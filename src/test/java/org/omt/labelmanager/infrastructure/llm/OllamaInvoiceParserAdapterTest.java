package org.omt.labelmanager.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OllamaInvoiceParserAdapterTest {

    private OllamaInvoiceParserAdapter adapter;
    private RestClient mockRestClient;
    private RestClient.RequestBodyUriSpec mockPost;
    private RestClient.RequestBodySpec mockUri;
    private RestClient.ResponseSpec mockResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockRestClient = mock(RestClient.class);
        mockPost = mock(RestClient.RequestBodyUriSpec.class);
        mockUri = mock(RestClient.RequestBodySpec.class, org.mockito.Answers.RETURNS_SELF);
        mockResponse = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.post()).thenReturn(mockPost);
        when(mockPost.uri(any(String.class))).thenReturn(mockUri);
        when(mockUri.retrieve()).thenReturn(mockResponse);

        RestClient.Builder mockBuilder = mock(RestClient.Builder.class);
        when(mockBuilder.baseUrl(any(String.class))).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockRestClient);

        var properties = new OllamaProperties("http://localhost:11434", "llava", "EUR");
        adapter = new OllamaInvoiceParserAdapter(
                mockBuilder, properties, JsonMapper.builder().build());
    }

    @Test
    void returnsEmptyDataForNullText() {
        var result = adapter.parse(null);

        assertThat(result.hasAnyData()).isFalse();
    }

    @Test
    void returnsEmptyDataForBlankText() {
        var result = adapter.parse("   ");

        assertThat(result.hasAnyData()).isFalse();
    }

    @Test
    void parsesCompleteInvoiceData() {
        mockOllamaResponse("""
                {
                  "netAmount": 100.00, "vatAmount": 21.00, "vatRate": 21,
                  "grossAmount": 121.00, "invoiceDate": "2024-01-15",
                  "invoiceReference": "INV-2024-001", "currency": "EUR"
                }
                """);

        var result = adapter.parse("Invoice text here");

        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.vatAmount()).isEqualByComparingTo(new BigDecimal("21.00"));
        assertThat(result.vatRate()).isEqualByComparingTo(new BigDecimal("21"));
        assertThat(result.grossAmount()).isEqualByComparingTo(new BigDecimal("121.00"));
        assertThat(result.invoiceDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.invoiceReference()).isEqualTo("INV-2024-001");
        assertThat(result.currency()).isEqualTo("EUR");
    }

    @Test
    void handlesPartialExtractionWithNulls() {
        mockOllamaResponse("""
                {
                  "netAmount": 100.00, "vatAmount": null, "vatRate": null,
                  "grossAmount": null, "invoiceDate": "2024-01-15",
                  "invoiceReference": null, "currency": null
                }
                """);

        var result = adapter.parse("Partial invoice");

        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.vatAmount()).isNull();
        assertThat(result.vatRate()).isNull();
        assertThat(result.grossAmount()).isNull();
        assertThat(result.invoiceDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.invoiceReference()).isNull();
        assertThat(result.currency()).isEqualTo("EUR"); // Falls back to default
    }

    @Test
    void extractsJsonFromResponseWithSurroundingText() {
        mockOllamaResponse("""
                Here is the extracted data:
                {"netAmount": 50.00, "grossAmount": 60.50}
                I hope this helps!
                """);

        var result = adapter.parse("Invoice with extra text");

        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.grossAmount()).isEqualByComparingTo(new BigDecimal("60.50"));
    }

    @Test
    void handlesNumbersAsStrings() {
        mockOllamaResponse("""
                {"netAmount": "100.00", "grossAmount": "121.00"}
                """);

        var result = adapter.parse("Invoice text");

        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.grossAmount()).isEqualByComparingTo(new BigDecimal("121.00"));
    }

    @Test
    void returnsEmptyWhenOllamaReturnsNoJson() {
        mockOllamaResponse("I could not find any invoice data in this text.");

        var result = adapter.parse("Random text");

        assertThat(result.hasAnyData()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsEmptyWhenOllamaCallFails() {
        when(mockResponse.body(any(Class.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var result = adapter.parse("Invoice text");

        assertThat(result.hasAnyData()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private void mockOllamaResponse(String responseText) {
        var ollamaResponse = new OllamaInvoiceParserAdapter.OllamaResponse(responseText);
        when(mockResponse.body(any(Class.class))).thenReturn(ollamaResponse);
    }
}
