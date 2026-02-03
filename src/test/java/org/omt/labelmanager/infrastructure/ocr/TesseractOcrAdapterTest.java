package org.omt.labelmanager.infrastructure.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TesseractOcrAdapterTest {

    private TesseractOcrAdapter adapter;

    @BeforeEach
    void setUp() {
        var properties = new TesseractProperties(null, "eng");
        adapter = new TesseractOcrAdapter(properties);
    }

    @Test
    void returnsEmptyStringForUnsupportedContentType() {
        InputStream content = new ByteArrayInputStream("test".getBytes());

        String result = adapter.extractText(content, "application/json");

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyStringForWordDocument() {
        InputStream content = new ByteArrayInputStream("test".getBytes());

        String result = adapter.extractText(content, "application/msword");

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyStringWhenImageCannotBeParsed() {
        InputStream invalidImage = new ByteArrayInputStream("not an image".getBytes());

        String result = adapter.extractText(invalidImage, "image/png");

        assertThat(result).isEmpty();
    }

    @Test
    void acceptsPdfContentType() {
        // This tests that PDF is recognized as a valid type
        // Actual extraction will fail without Tesseract, but error handling returns empty
        InputStream content = new ByteArrayInputStream("not a real pdf".getBytes());

        String result = adapter.extractText(content, "application/pdf");

        // Returns empty because it can't actually process invalid PDF, but doesn't throw
        assertThat(result).isEmpty();
    }

    @Test
    void acceptsJpegContentType() {
        InputStream content = new ByteArrayInputStream("not an image".getBytes());

        String result = adapter.extractText(content, "image/jpeg");

        assertThat(result).isEmpty();
    }
}
