package org.omt.labelmanager.finance.cost.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.finance.cost.api.CostCommandApi;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.finance.cost.domain.CostOwner;
import org.omt.labelmanager.finance.cost.domain.CostType;
import org.omt.labelmanager.finance.cost.domain.VatAmount;
import org.omt.labelmanager.finance.shared.DocumentUpload;
import org.omt.labelmanager.finance.shared.RetrievedDocument;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.shared.Money;
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

    private static final Long LABEL_ID = 1L;
    private static final Long RELEASE_ID = 42L;
    private static final Long COST_ID = 99L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CostCommandApi costCommandApi;

    @MockitoBean private CostQueryApi costQueryApi;

    @MockitoBean private ReleaseQueryApi releaseQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    private void releaseBelongsToLabel() {
        when(releaseQueryApi.belongsToLabel(RELEASE_ID, LABEL_ID)).thenReturn(true);
    }

    private void costOwnedBy(CostOwner owner) {
        when(costQueryApi.findById(COST_ID))
                .thenReturn(
                        Optional.of(
                                new Cost(
                                        COST_ID,
                                        Money.of(new BigDecimal("100.00")),
                                        new VatAmount(
                                                Money.of(new BigDecimal("25.00")),
                                                new BigDecimal("0.25")),
                                        Money.of(new BigDecimal("125.00")),
                                        CostType.MASTERING,
                                        LocalDate.of(2024, 6, 15),
                                        "A cost",
                                        owner,
                                        null,
                                        null)));
    }

    @Test
    void registerCost_withReleaseId_ownsTheCostByTheRelease() throws Exception {
        releaseBelongsToLabel();

        mockMvc.perform(
                        multipart("/api/labels/1/costs")
                                .with(user(testUser))
                                .with(csrf())
                                .param("releaseId", "42")
                                .param("netAmount", "100.00")
                                .param("vatAmount", "25.00")
                                .param("vatRate", "0.25")
                                .param("grossAmount", "125.00")
                                .param("costType", "MASTERING")
                                .param("incurredOn", "2024-06-15")
                                .param("description", "Mastering for album")
                                .param("documentReference", "INV-2024-001"))
                .andExpect(status().isCreated());

        verify(costCommandApi)
                .registerCost(
                        eq(Money.of(new BigDecimal("100.00"))),
                        eq(
                                new VatAmount(
                                        Money.of(new BigDecimal("25.00")), new BigDecimal("0.25"))),
                        eq(Money.of(new BigDecimal("125.00"))),
                        eq(CostType.MASTERING),
                        eq(LocalDate.of(2024, 6, 15)),
                        eq("Mastering for album"),
                        eq(CostOwner.release(RELEASE_ID)),
                        eq("INV-2024-001"),
                        isNull());
    }

    @Test
    void registerCost_withoutReleaseId_ownsTheCostByTheLabel() throws Exception {
        mockMvc.perform(
                        multipart("/api/labels/1/costs")
                                .with(user(testUser))
                                .with(csrf())
                                .param("netAmount", "50.00")
                                .param("vatAmount", "12.50")
                                .param("vatRate", "0.25")
                                .param("grossAmount", "62.50")
                                .param("costType", "HOSTING")
                                .param("incurredOn", "2024-07-01")
                                .param("description", "Website hosting"))
                .andExpect(status().isCreated());

        verify(costCommandApi)
                .registerCost(
                        eq(Money.of(new BigDecimal("50.00"))),
                        eq(
                                new VatAmount(
                                        Money.of(new BigDecimal("12.50")), new BigDecimal("0.25"))),
                        eq(Money.of(new BigDecimal("62.50"))),
                        eq(CostType.HOSTING),
                        eq(LocalDate.of(2024, 7, 1)),
                        eq("Website hosting"),
                        eq(CostOwner.label(LABEL_ID)),
                        isNull(),
                        isNull());
    }

    @Test
    void registerCost_rejectsAReleaseBelongingToAnotherLabel() throws Exception {
        when(releaseQueryApi.belongsToLabel(RELEASE_ID, LABEL_ID)).thenReturn(false);

        mockMvc.perform(
                        multipart("/api/labels/1/costs")
                                .with(user(testUser))
                                .with(csrf())
                                .param("releaseId", "42")
                                .param("netAmount", "100.00")
                                .param("vatAmount", "25.00")
                                .param("vatRate", "0.25")
                                .param("grossAmount", "125.00")
                                .param("costType", "MASTERING")
                                .param("incurredOn", "2024-06-15")
                                .param("description", "Mastering for album"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(costCommandApi);
    }

    @Test
    void registerCost_withDocumentUpload() throws Exception {
        releaseBelongsToLabel();
        MockMultipartFile document =
                new MockMultipartFile(
                        "document", "invoice.pdf", "application/pdf", "PDF content".getBytes());

        mockMvc.perform(
                        multipart("/api/labels/1/costs")
                                .file(document)
                                .with(user(testUser))
                                .with(csrf())
                                .param("releaseId", "42")
                                .param("netAmount", "100.00")
                                .param("vatAmount", "25.00")
                                .param("vatRate", "0.25")
                                .param("grossAmount", "125.00")
                                .param("costType", "MASTERING")
                                .param("incurredOn", "2024-06-15")
                                .param("description", "Mastering for album"))
                .andExpect(status().isCreated());

        verify(costCommandApi)
                .registerCost(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(CostOwner.release(RELEASE_ID)),
                        isNull(),
                        argThat(
                                (DocumentUpload doc) ->
                                        doc != null
                                                && "invoice.pdf".equals(doc.filename())
                                                && "application/pdf".equals(doc.contentType())));
    }

    @Test
    void registerCost_rejectsInvalidDocumentType() throws Exception {
        MockMultipartFile document =
                new MockMultipartFile(
                        "document",
                        "script.js",
                        "application/javascript",
                        "alert('bad')".getBytes());

        mockMvc.perform(
                        multipart("/api/labels/1/costs")
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
    void getDocument_returnsDocumentInlineByDefault() throws Exception {
        costOwnedBy(CostOwner.label(LABEL_ID));
        byte[] content = "PDF content".getBytes();
        RetrievedDocument document =
                new RetrievedDocument(
                        new ByteArrayInputStream(content),
                        "application/pdf",
                        "invoice.pdf",
                        content.length);
        when(costCommandApi.retrieveDocument(COST_ID)).thenReturn(Optional.of(document));

        mockMvc.perform(get("/api/labels/1/costs/99/document").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(
                        header().string("Content-Disposition", "inline; filename=\"invoice.pdf\""))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(content));
    }

    @Test
    void getDocument_returnsDocumentAsAttachmentWhenDownload() throws Exception {
        costOwnedBy(CostOwner.label(LABEL_ID));
        byte[] content = "PDF content".getBytes();
        RetrievedDocument document =
                new RetrievedDocument(
                        new ByteArrayInputStream(content),
                        "application/pdf",
                        "invoice.pdf",
                        content.length);
        when(costCommandApi.retrieveDocument(COST_ID)).thenReturn(Optional.of(document));

        mockMvc.perform(
                        get("/api/labels/1/costs/99/document")
                                .param("action", "download")
                                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"invoice.pdf\""));
    }

    @Test
    void getDocument_reachesACostOwnedByAReleaseOfThisLabel() throws Exception {
        costOwnedBy(CostOwner.release(RELEASE_ID));
        releaseBelongsToLabel();
        byte[] content = "PDF content".getBytes();
        when(costCommandApi.retrieveDocument(COST_ID))
                .thenReturn(
                        Optional.of(
                                new RetrievedDocument(
                                        new ByteArrayInputStream(content),
                                        "application/pdf",
                                        "invoice.pdf",
                                        content.length)));

        mockMvc.perform(get("/api/labels/1/costs/99/document").with(user(testUser)))
                .andExpect(status().isOk());
    }

    @Test
    void getDocument_returns404WhenTheCostBelongsToAnotherLabel() throws Exception {
        costOwnedBy(CostOwner.label(7L));

        mockMvc.perform(get("/api/labels/1/costs/99/document").with(user(testUser)))
                .andExpect(status().isNotFound());

        verify(costCommandApi, org.mockito.Mockito.never()).retrieveDocument(any());
    }

    @Test
    void getDocument_returns404WhenCostNotFound() throws Exception {
        when(costQueryApi.findById(COST_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labels/1/costs/99/document").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDocument_returns404WhenNoDocumentAttached() throws Exception {
        costOwnedBy(CostOwner.label(LABEL_ID));
        when(costCommandApi.retrieveDocument(COST_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labels/1/costs/99/document").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCost_callsUseCaseAndReturnsNoContent() throws Exception {
        costOwnedBy(CostOwner.label(LABEL_ID));
        when(costCommandApi.deleteCost(COST_ID)).thenReturn(true);

        mockMvc.perform(delete("/api/labels/1/costs/99").with(user(testUser)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(costCommandApi).deleteCost(COST_ID);
    }

    @Test
    void deleteCost_returns404WhenTheCostBelongsToAnotherLabel() throws Exception {
        costOwnedBy(CostOwner.label(7L));

        mockMvc.perform(delete("/api/labels/1/costs/99").with(user(testUser)).with(csrf()))
                .andExpect(status().isNotFound());

        verify(costCommandApi, org.mockito.Mockito.never()).deleteCost(any());
    }

    @Test
    void updateCost_callsUseCaseAndReturnsNoContent() throws Exception {
        costOwnedBy(CostOwner.release(RELEASE_ID));
        releaseBelongsToLabel();
        when(costCommandApi.updateCost(
                        any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        mockMvc.perform(
                        multipart("/api/labels/1/costs/99")
                                .with(
                                        req -> {
                                            req.setMethod("PUT");
                                            return req;
                                        })
                                .with(user(testUser))
                                .with(csrf())
                                .param("netAmount", "200.00")
                                .param("vatAmount", "50.00")
                                .param("vatRate", "0.25")
                                .param("grossAmount", "250.00")
                                .param("costType", "MIXING")
                                .param("incurredOn", "2024-07-15")
                                .param("description", "Mixing updated")
                                .param("documentReference", "INV-2024-002"))
                .andExpect(status().isNoContent());

        verify(costCommandApi)
                .updateCost(
                        eq(COST_ID),
                        eq(Money.of(new BigDecimal("200.00"))),
                        eq(
                                new VatAmount(
                                        Money.of(new BigDecimal("50.00")), new BigDecimal("0.25"))),
                        eq(Money.of(new BigDecimal("250.00"))),
                        eq(CostType.MIXING),
                        eq(LocalDate.of(2024, 7, 15)),
                        eq("Mixing updated"),
                        eq("INV-2024-002"),
                        isNull());
    }

    @Test
    void updateCost_returns404WhenTheCostIsOwnedByAUser() throws Exception {
        costOwnedBy(CostOwner.user(3L));

        mockMvc.perform(
                        multipart("/api/labels/1/costs/99")
                                .with(
                                        req -> {
                                            req.setMethod("PUT");
                                            return req;
                                        })
                                .with(user(testUser))
                                .with(csrf())
                                .param("netAmount", "75.00")
                                .param("vatAmount", "18.75")
                                .param("vatRate", "0.25")
                                .param("grossAmount", "93.75")
                                .param("costType", "MARKETING")
                                .param("incurredOn", "2024-08-01")
                                .param("description", "Marketing campaign"))
                .andExpect(status().isNotFound());
    }

    @Test
    void costs_withoutReleaseId_returnsTheLabelsCosts() throws Exception {
        when(costQueryApi.getCostsForLabel(LABEL_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/labels/1/costs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(costQueryApi).getCostsForLabel(LABEL_ID);
    }

    @Test
    void costs_withReleaseId_returnsThatReleasesCosts() throws Exception {
        releaseBelongsToLabel();
        when(costQueryApi.getCostsForRelease(RELEASE_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/labels/1/costs?releaseId=42").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(costQueryApi).getCostsForRelease(RELEASE_ID);
    }

    @Test
    void costs_returns404WhenReleaseBelongsToAnotherLabel() throws Exception {
        when(releaseQueryApi.belongsToLabel(RELEASE_ID, LABEL_ID)).thenReturn(false);

        mockMvc.perform(get("/api/labels/1/costs?releaseId=42").with(user(testUser)))
                .andExpect(status().isNotFound());

        verify(costQueryApi, org.mockito.Mockito.never()).getCostsForRelease(any());
    }
}
