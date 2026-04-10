package org.omt.labelmanager.finance.extraction.infrastructure;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("invoice.parser")
@Validated
record InvoiceParserProperties(
        @NotBlank String url,
        @NotBlank String apiKey
) {}
