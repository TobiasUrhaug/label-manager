package org.omt.labelmanager.finance.api.cost;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.application.DocumentUpload;
import org.omt.labelmanager.finance.application.RegisterCostUseCase;
import org.omt.labelmanager.finance.application.RetrieveCostDocumentUseCase;
import org.omt.labelmanager.finance.application.RetrievedDocument;
import org.omt.labelmanager.finance.domain.cost.CostOwner;
import org.omt.labelmanager.finance.domain.cost.CostType;
import org.omt.labelmanager.finance.domain.cost.VatAmount;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CostController.class)
@Import(TestSecurityConfig.class)
class CostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterCostUseCase registerCostUseCase;

    @MockitoBean
    private RetrieveCostDocumentUseCase retrieveCostDocumentUseCase;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void registerCostForRelease_callsUseCaseAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/42/costs")
                        .with(user(testUser))
                        .with(csrf())
                        .param("netAmount", "100.00")
                        .param("vatAmount", "25.00")
                        .param("vatRate", "0.25")
                        .param("grossAmount", "125.00")
                        .param("costType", "MASTERING")
                        .param("incurredOn", "2024-06-15")
                        .param("description", "Mastering for album")
                        .param("documentReference", "INV-2024-001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/42"));

        verify(registerCostUseCase).registerCost(
                eq(Money.of(new BigDecimal("100.00"))),
                eq(new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25"))),
                eq(Money.of(new BigDecimal("125.00"))),
                eq(CostType.MASTERING),
                eq(LocalDate.of(2024, 6, 15)),
                eq("Mastering for album"),
                eq(CostOwner.release(42L)),
                eq("INV-2024-001"),
                isNull()
        );
    }

    @Test
    void registerCostForLabel_callsUseCaseAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels/10/costs")
                        .with(user(testUser))
                        .with(csrf())
                        .param("netAmount", "50.00")
                        .param("vatAmount", "12.50")
                        .param("vatRate", "0.25")
                        .param("grossAmount", "62.50")
                        .param("costType", "HOSTING")
                        .param("incurredOn", "2024-07-01")
                        .param("description", "Website hosting"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/10"));

        verify(registerCostUseCase).registerCost(
                eq(Money.of(new BigDecimal("50.00"))),
                eq(new VatAmount(Money.of(new BigDecimal("12.50")), new BigDecimal("0.25"))),
                eq(Money.of(new BigDecimal("62.50"))),
                eq(CostType.HOSTING),
                eq(LocalDate.of(2024, 7, 1)),
                eq("Website hosting"),
                eq(CostOwner.label(10L)),
                isNull(),
                isNull()
        );
    }

    @Test
    void registerCostForRelease_withDocumentUpload() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "invoice.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        mockMvc
                .perform(multipart("/labels/1/releases/42/costs")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf())
                        .param("netAmount", "100.00")
                        .param("vatAmount", "25.00")
                        .param("vatRate", "0.25")
                        .param("grossAmount", "125.00")
                        .param("costType", "MASTERING")
                        .param("incurredOn", "2024-06-15")
                        .param("description", "Mastering for album"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/42"));

        verify(registerCostUseCase).registerCost(
                eq(Money.of(new BigDecimal("100.00"))),
                eq(new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25"))),
                eq(Money.of(new BigDecimal("125.00"))),
                eq(CostType.MASTERING),
                eq(LocalDate.of(2024, 6, 15)),
                eq("Mastering for album"),
                eq(CostOwner.release(42L)),
                isNull(),
                argThat((DocumentUpload doc) ->
                        doc != null
                        && "invoice.pdf".equals(doc.filename())
                        && "application/pdf".equals(doc.contentType()))
        );
    }

    @Test
    void registerCostForRelease_rejectsInvalidDocumentType() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "script.js",
                "application/javascript",
                "alert('bad')".getBytes()
        );

        mockMvc
                .perform(multipart("/labels/1/releases/42/costs")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf())
                        .param("netAmount", "100.00")
                        .param("vatAmount", "25.00")
                        .param("vatRate", "0.25")
                        .param("grossAmount", "125.00")
                        .param("costType", "MASTERING")
                        .param("incurredOn", "2024-06-15")
                        .param("description", "Mastering for album"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerCostForLabel_withImageUpload() throws Exception {
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "receipt.png",
                "image/png",
                "PNG content".getBytes()
        );

        mockMvc
                .perform(multipart("/labels/10/costs")
                        .file(document)
                        .with(user(testUser))
                        .with(csrf())
                        .param("netAmount", "50.00")
                        .param("vatAmount", "12.50")
                        .param("vatRate", "0.25")
                        .param("grossAmount", "62.50")
                        .param("costType", "HOSTING")
                        .param("incurredOn", "2024-07-01")
                        .param("description", "Website hosting"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/10"));

        verify(registerCostUseCase).registerCost(
                any(), any(), any(), any(), any(), any(), any(), any(),
                argThat((DocumentUpload doc) ->
                        doc != null
                        && "receipt.png".equals(doc.filename())
                        && "image/png".equals(doc.contentType()))
        );
    }

    @Test
    void getDocument_returnsDocumentInlineByDefault() throws Exception {
        byte[] content = "PDF content".getBytes();
        RetrievedDocument document = new RetrievedDocument(
                new ByteArrayInputStream(content),
                "application/pdf",
                "invoice.pdf",
                content.length
        );
        when(retrieveCostDocumentUseCase.retrieveDocument(1L)).thenReturn(Optional.of(document));

        mockMvc
                .perform(get("/costs/1/document")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"invoice.pdf\""))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(content));
    }

    @Test
    void getDocument_returnsDocumentAsAttachmentWhenDownload() throws Exception {
        byte[] content = "PDF content".getBytes();
        RetrievedDocument document = new RetrievedDocument(
                new ByteArrayInputStream(content),
                "application/pdf",
                "invoice.pdf",
                content.length
        );
        when(retrieveCostDocumentUseCase.retrieveDocument(1L)).thenReturn(Optional.of(document));

        mockMvc
                .perform(get("/costs/1/document")
                        .param("action", "download")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"invoice.pdf\""));
    }

    @Test
    void getDocument_returns404WhenCostNotFound() throws Exception {
        when(retrieveCostDocumentUseCase.retrieveDocument(999L)).thenReturn(Optional.empty());

        mockMvc
                .perform(get("/costs/999/document")
                        .with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDocument_returns404WhenNoDocumentAttached() throws Exception {
        when(retrieveCostDocumentUseCase.retrieveDocument(1L)).thenReturn(Optional.empty());

        mockMvc
                .perform(get("/costs/1/document")
                        .with(user(testUser)))
                .andExpect(status().isNotFound());
    }
}
