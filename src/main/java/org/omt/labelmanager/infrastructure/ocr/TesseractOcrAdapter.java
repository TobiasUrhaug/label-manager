package org.omt.labelmanager.infrastructure.ocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.omt.labelmanager.finance.application.extraction.OcrPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TesseractOcrAdapter implements OcrPort {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrAdapter.class);

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg"
    );

    private static final String PDF_TYPE = "application/pdf";

    private final TesseractProperties properties;

    public TesseractOcrAdapter(TesseractProperties properties) {
        this.properties = properties;
    }

    @Override
    public String extractText(InputStream content, String contentType) {
        log.info("Extracting text from document with content type '{}'", contentType);

        try {
            ITesseract tesseract = createTesseract();

            if (PDF_TYPE.equals(contentType)) {
                return extractFromPdf(tesseract, content);
            } else if (IMAGE_TYPES.contains(contentType)) {
                return extractFromImage(tesseract, content);
            } else {
                log.warn("Unsupported content type for OCR: {}", contentType);
                return "";
            }
        } catch (Exception e) {
            log.error("OCR extraction failed: {}", e.getMessage());
            return "";
        }
    }

    private ITesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        if (properties.dataPath() != null && !properties.dataPath().isBlank()) {
            tesseract.setDatapath(properties.dataPath());
        }
        tesseract.setLanguage(properties.language());
        return tesseract;
    }

    private String extractFromPdf(ITesseract tesseract, InputStream content)
            throws IOException, TesseractException {
        Path tempFile = Files.createTempFile("invoice-", ".pdf");
        try {
            Files.copy(content, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            File pdfFile = tempFile.toFile();

            log.debug("Processing PDF file for OCR");
            String result = tesseract.doOCR(pdfFile);
            log.debug("Extracted {} characters from PDF", result.length());

            return result.trim();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String extractFromImage(ITesseract tesseract, InputStream content)
            throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(content);
        if (image == null) {
            log.warn("Could not read image from input stream");
            return "";
        }

        log.debug("Processing image for OCR ({}x{})", image.getWidth(), image.getHeight());
        String result = tesseract.doOCR(image);
        log.debug("Extracted {} characters from image", result.length());

        return result.trim();
    }
}
