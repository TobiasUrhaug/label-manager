package org.omt.labelmanager.finance.extraction.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;

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
                .andRespond(withSuccess("""
                        {
                            "invoiceDate": "2024-03-15",
                            "invoiceReference": "INV-001",
                            "netAmount": {"amount": "1000.00", "currency": "NOK"},
                            "vatAmount": {"amount": "250.00", "currency": "NOK"},
                            "totalAmount": {"amount": "1250.00", "currency": "NOK"}
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "application/pdf");

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
                .andRespond(withSuccess("""
                        {
                            "invoiceDate": null,
                            "invoiceReference": "INV-002",
                            "netAmount": null,
                            "vatAmount": null,
                            "totalAmount": null
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = adapter.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "application/pdf");

        assertThat(result.invoiceDate()).isNull();
        assertThat(result.invoiceReference()).isEqualTo("INV-002");
        assertThat(result.netAmount()).isNull();
        assertThat(result.vatAmount()).isNull();
        assertThat(result.grossAmount()).isNull();
        assertThat(result.currency()).isNull();
        assertThat(result.vatRate()).isNull();
    }

    @Test
    void returnsEmptyData_whenExternalServiceReturns400() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        var result = adapter.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "application/pdf");

        assertThat(result).isEqualTo(ExtractedInvoiceData.empty());
    }

    @Test
    void returnsEmptyData_whenExternalServiceReturns503() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServiceUnavailable());

        var result = adapter.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "application/pdf");

        assertThat(result).isEqualTo(ExtractedInvoiceData.empty());
    }

    @Test
    void returnsEmptyData_whenNetworkErrorOccurs() {
        server.expect(requestTo("http://test/api/v1/extract"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> { throw new IOException("Connection refused"); });

        var result = adapter.extract(new ByteArrayInputStream(new byte[]{1, 2, 3}), "application/pdf");

        assertThat(result).isEqualTo(ExtractedInvoiceData.empty());
    }
}
