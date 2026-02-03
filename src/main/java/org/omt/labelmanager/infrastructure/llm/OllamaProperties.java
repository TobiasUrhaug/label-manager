package org.omt.labelmanager.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "extraction.ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        String defaultCurrency
) {

    public OllamaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        if (model == null || model.isBlank()) {
            model = "llava";
        }
        if (defaultCurrency == null || defaultCurrency.isBlank()) {
            defaultCurrency = "EUR";
        }
    }
}
