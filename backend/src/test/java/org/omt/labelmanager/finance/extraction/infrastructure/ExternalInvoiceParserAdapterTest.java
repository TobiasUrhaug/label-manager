package org.omt.labelmanager.finance.extraction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.extraction.api.InvoiceParserUnavailableException;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

class ExternalInvoiceParserAdapterTest {

    private MockRestServiceServer server;
    private ExternalInvoiceParserAdapter adapter;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        RestClient restClient = RestClient.builder(restTemplate).baseUrl("http://test").build();
        adapter = new ExternalInvoiceParserAdapter(restClient);
    }

    @Test
    void mapsFullResponseToExtractedInvoiceData() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                """
                        {
                            "invoiceDate": "2024-03-15",
                            "invoiceReference": "INV-001",
                            "netAmount": {"amount": "1000.00", "currency": "NOK"},
                            "vatAmount": {"amount": "250.00", "currency": "NOK"},
                            "totalAmount": {"amount": "1250.00", "currency": "NOK"}
                        }
                        """,
                                MediaType.APPLICATION_JSON));

        var result =
                adapter.extract(new ByteArrayInputStream(new byte[] {1, 2, 3}), "application/pdf");

        assertThat(result.invoiceDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(result.invoiceReference()).isEqualTo("INV-001");
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.vatAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.grossAmount()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(result.currency()).isEqualTo("NOK");
        assertThat(result.vatRate()).isNull();
    }

    @Test
    void propagatesNullFields_whenResponseIsPartial() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                """
                        {
                            "invoiceDate": null,
                            "invoiceReference": "INV-002",
                            "netAmount": null,
                            "vatAmount": null,
                            "totalAmount": null
                        }
                        """,
                                MediaType.APPLICATION_JSON));

        var result =
                adapter.extract(new ByteArrayInputStream(new byte[] {1, 2, 3}), "application/pdf");

        assertThat(result.invoiceDate()).isNull();
        assertThat(result.invoiceReference()).isEqualTo("INV-002");
        assertThat(result.netAmount()).isNull();
        assertThat(result.vatAmount()).isNull();
        assertThat(result.grossAmount()).isNull();
        assertThat(result.currency()).isNull();
        assertThat(result.vatRate()).isNull();
    }

    @Test
    void throwsUnavailable_whenExternalServiceReturns400() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        assertThatThrownBy(
                        () ->
                                adapter.extract(
                                        new ByteArrayInputStream(new byte[] {1, 2, 3}),
                                        "application/pdf"))
                .isInstanceOf(InvoiceParserUnavailableException.class)
                .hasMessageContaining("400");
    }

    @Test
    void throwsUnavailable_whenExternalServiceReturns503() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServiceUnavailable());

        assertThatThrownBy(
                        () ->
                                adapter.extract(
                                        new ByteArrayInputStream(new byte[] {1, 2, 3}),
                                        "application/pdf"))
                .isInstanceOf(InvoiceParserUnavailableException.class)
                .hasMessageContaining("503");
    }

    @Test
    void throwsUnavailable_whenNetworkErrorOccurs() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        request -> {
                            throw new IOException("Connection refused");
                        });

        assertThatThrownBy(
                        () ->
                                adapter.extract(
                                        new ByteArrayInputStream(new byte[] {1, 2, 3}),
                                        "application/pdf"))
                .isInstanceOf(InvoiceParserUnavailableException.class)
                .hasMessageContaining("could not be reached");
    }

    @Test
    void throwsUnreadable_whenTheParserAnswersWithAnUnparseableAmount() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                """
                        {
                            "invoiceReference": "INV-003",
                            "netAmount": {"amount": "1.234,00", "currency": "NOK"}
                        }
                        """,
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(
                        () ->
                                adapter.extract(
                                        new ByteArrayInputStream(new byte[] {1, 2, 3}),
                                        "application/pdf"))
                .isInstanceOf(InvoiceParserUnavailableException.class)
                .hasMessageContaining("could not be read")
                .hasMessageNotContaining("could not be reached");
    }

    @Test
    void returnsEmptyData_whenTheParserAnswersWithNoBody() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        var result =
                adapter.extract(new ByteArrayInputStream(new byte[] {1, 2, 3}), "application/pdf");

        assertThat(result).isEqualTo(ExtractedInvoiceData.empty());
        assertThat(result.hasAnyData()).isFalse();
    }
}
