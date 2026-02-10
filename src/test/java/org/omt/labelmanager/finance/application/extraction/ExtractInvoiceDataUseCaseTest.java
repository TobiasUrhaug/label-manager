package org.omt.labelmanager.finance.application.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.omt.labelmanager.finance.extraction.infrastructure.InvoiceParserPort;
import org.omt.labelmanager.finance.extraction.infrastructure.OcrPort;

class ExtractInvoiceDataUseCaseTest {

    private OcrPort ocrPort;
    private InvoiceParserPort invoiceParserPort;
    private ExtractInvoiceDataUseCase useCase;

    @BeforeEach
    void setUp() {
        ocrPort = mock(OcrPort.class);
        invoiceParserPort = mock(InvoiceParserPort.class);
        useCase = new ExtractInvoiceDataUseCase(ocrPort, invoiceParserPort);
    }

    @Test
    void extractsDataFromDocumentSuccessfully() {
        InputStream content = new ByteArrayInputStream("pdf content".getBytes());
        String contentType = "application/pdf";
        String ocrText = "Invoice #123\nAmount: 100.00 EUR";

        when(ocrPort.extractText(any(), eq(contentType))).thenReturn(ocrText);
        when(invoiceParserPort.parse(ocrText)).thenReturn(new ExtractedInvoiceData(
                new BigDecimal("100.00"),
                new BigDecimal("21.00"),
                new BigDecimal("21"),
                new BigDecimal("121.00"),
                LocalDate.of(2024, 1, 15),
                "INV-123",
                "EUR"
        ));

        ExtractedInvoiceData result = useCase.extract(content, contentType);

        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.invoiceReference()).isEqualTo("INV-123");
        verify(ocrPort).extractText(any(), eq(contentType));
        verify(invoiceParserPort).parse(ocrText);
    }

    @Test
    void returnsEmptyDataWhenOcrExtractsNoText() {
        InputStream content = new ByteArrayInputStream("image".getBytes());
        String contentType = "image/png";

        when(ocrPort.extractText(any(), eq(contentType))).thenReturn("");

        ExtractedInvoiceData result = useCase.extract(content, contentType);

        assertThat(result.hasAnyData()).isFalse();
        verify(invoiceParserPort, never()).parse(any());
    }

    @Test
    void returnsEmptyDataWhenOcrReturnsNull() {
        InputStream content = new ByteArrayInputStream("image".getBytes());
        String contentType = "image/png";

        when(ocrPort.extractText(any(), eq(contentType))).thenReturn(null);

        ExtractedInvoiceData result = useCase.extract(content, contentType);

        assertThat(result.hasAnyData()).isFalse();
        verify(invoiceParserPort, never()).parse(any());
    }

    @Test
    void returnsEmptyDataWhenOcrReturnsBlankText() {
        InputStream content = new ByteArrayInputStream("image".getBytes());
        String contentType = "image/png";

        when(ocrPort.extractText(any(), eq(contentType))).thenReturn("   \n  ");

        ExtractedInvoiceData result = useCase.extract(content, contentType);

        assertThat(result.hasAnyData()).isFalse();
        verify(invoiceParserPort, never()).parse(any());
    }

    @Test
    void returnsEmptyDataWhenParserCannotExtractFields() {
        InputStream content = new ByteArrayInputStream("pdf".getBytes());
        String contentType = "application/pdf";
        String ocrText = "Some random text without invoice data";

        when(ocrPort.extractText(any(), eq(contentType))).thenReturn(ocrText);
        when(invoiceParserPort.parse(ocrText)).thenReturn(ExtractedInvoiceData.empty());

        ExtractedInvoiceData result = useCase.extract(content, contentType);

        assertThat(result.hasAnyData()).isFalse();
    }

    @Test
    void handlesPartialExtraction() {
        InputStream content = new ByteArrayInputStream("pdf".getBytes());
        String contentType = "application/pdf";
        String ocrText = "Partial invoice text";

        when(ocrPort.extractText(any(), eq(contentType))).thenReturn(ocrText);
        when(invoiceParserPort.parse(ocrText)).thenReturn(new ExtractedInvoiceData(
                new BigDecimal("50.00"),
                null,
                null,
                null,
                null,
                null,
                "EUR"
        ));

        ExtractedInvoiceData result = useCase.extract(content, contentType);

        assertThat(result.hasAnyData()).isTrue();
        assertThat(result.netAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.vatAmount()).isNull();
    }
}
