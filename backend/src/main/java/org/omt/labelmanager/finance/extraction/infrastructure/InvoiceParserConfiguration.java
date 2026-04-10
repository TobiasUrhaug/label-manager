package org.omt.labelmanager.finance.extraction.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(InvoiceParserProperties.class)
class InvoiceParserConfiguration {

    // Note: X-Api-Key is set as a default header on every request.
    // Do not enable DEBUG logging for org.springframework.web.client in production —
    // Spring logs request headers at that level, which would expose the API key.
    @Bean("invoiceParserRestClient")
    RestClient invoiceParserRestClient(InvoiceParserProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.url())
                .defaultHeader("X-Api-Key", properties.apiKey())
                .build();
    }
}
