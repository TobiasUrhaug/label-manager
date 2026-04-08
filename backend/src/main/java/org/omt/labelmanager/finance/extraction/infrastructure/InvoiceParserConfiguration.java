package org.omt.labelmanager.finance.extraction.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(InvoiceParserProperties.class)
class InvoiceParserConfiguration {

    @Bean("invoiceParserRestClient")
    RestClient invoiceParserRestClient(InvoiceParserProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.url())
                .defaultHeader("X-Api-Key", properties.apiKey())
                .build();
    }
}
