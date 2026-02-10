package org.omt.labelmanager.finance.api.extraction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.extraction.application.ExtractInvoiceDataUseCase;
import org.omt.labelmanager.finance.extraction.domain.ExtractedInvoiceData;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvoiceExtractionController.class)
@Import(TestSecurityConfig.class)
class InvoiceExtractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtractInvoiceDataUseCase extractInvoiceDataUseCase;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void extractsInvoiceDataFromPdf() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        when(extractInvoiceDataUseCase.extract(any(), eq("application/pdf")))
                .thenReturn(new ExtractedInvoiceData(
                        new BigDecimal("100.00"),
                        new BigDecimal("21.00"),
                        new BigDecimal("21"),
                        new BigDecimal("121.00"),
                        LocalDate.of(2024, 1, 15),
                        "INV-2024-001",
                        "EUR"
                ));

        mockMvc.perform(multipart("/api/costs/extract")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").value(100.00))
                .andExpect(jsonPath("$.vatAmount").value(21.00))
                .andExpect(jsonPath("$.vatRate").value(21))
                .andExpect(jsonPath("$.grossAmount").value(121.00))
                .andExpect(jsonPath("$.invoiceDate").value("2024-01-15"))
                .andExpect(jsonPath("$.invoiceReference").value("INV-2024-001"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void extractsInvoiceDataFromImage() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.png",
                "image/png",
                "image content".getBytes()
        );

        when(extractInvoiceDataUseCase.extract(any(), eq("image/png")))
                .thenReturn(new ExtractedInvoiceData(
                        new BigDecimal("50.00"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "EUR"
                ));

        mockMvc.perform(multipart("/api/costs/extract")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").value(50.00))
                .andExpect(jsonPath("$.vatAmount").isEmpty())
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void returnsBadRequestForUnsupportedContentType() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.doc",
                "application/msword",
                "doc content".getBytes()
        );

        mockMvc.perform(multipart("/api/costs/extract")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsBadRequestWhenNoDocumentProvided() throws Exception {
        mockMvc.perform(multipart("/api/costs/extract")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsEmptyDataWhenExtractionFails() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        when(extractInvoiceDataUseCase.extract(any(), eq("application/pdf")))
                .thenReturn(ExtractedInvoiceData.empty());

        mockMvc.perform(multipart("/api/costs/extract")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAmount").isEmpty())
                .andExpect(jsonPath("$.invoiceReference").isEmpty());
    }

}
