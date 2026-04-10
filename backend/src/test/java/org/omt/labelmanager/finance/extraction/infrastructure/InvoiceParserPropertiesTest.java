package org.omt.labelmanager.finance.extraction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InvoiceParserPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(InvoiceParserConfiguration.class);

    @Test
    void contextFailsToLoad_whenUrlPropertyIsMissing() {
        contextRunner
                .withPropertyValues("invoice.parser.api-key=test-key")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextFailsToLoad_whenApiKeyPropertyIsMissing() {
        contextRunner
                .withPropertyValues("invoice.parser.url=http://test-parser")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void contextLoads_whenBothPropertiesArePresent() {
        contextRunner
                .withPropertyValues(
                        "invoice.parser.url=http://test-parser",
                        "invoice.parser.api-key=test-key"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }
}
