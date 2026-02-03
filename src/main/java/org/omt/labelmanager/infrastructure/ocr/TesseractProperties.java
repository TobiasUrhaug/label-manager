package org.omt.labelmanager.infrastructure.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "extraction.tesseract")
public record TesseractProperties(
        String dataPath,
        String language
) {

    public TesseractProperties {
        if (language == null || language.isBlank()) {
            language = "eng";
        }
    }
}
