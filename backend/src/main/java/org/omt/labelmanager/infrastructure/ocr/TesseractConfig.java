package org.omt.labelmanager.infrastructure.ocr;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TesseractProperties.class)
public class TesseractConfig {
}
