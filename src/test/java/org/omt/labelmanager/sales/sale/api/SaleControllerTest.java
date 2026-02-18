package org.omt.labelmanager.sales.sale.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SaleController.class)
@Import(TestSecurityConfig.class)
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SaleCommandApi saleCommandApi;

    @MockitoBean
    private SaleQueryApi saleQueryApi;

    @MockitoBean
    private LabelQueryApi labelQueryApi;

    @MockitoBean
    private ReleaseQueryApi releaseQueryApi;

    @MockitoBean
    private DistributorQueryApi distributorQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    private static final Long LABEL_ID = 1L;
    private static final Long SALE_ID = 42L;
    private static final Long RELEASE_ID = 10L;

    private Label testLabel;
    private Sale testSale;

    @BeforeEach
    void setUp() {
        testLabel = new Label(LABEL_ID, "Test Label", null, null, null, null, 1L);

        var lineItem = new SaleLineItem(
                1L, RELEASE_ID,
                org.omt.labelmanager.catalog.release.domain.ReleaseFormat.VINYL,
                5,
                Money.of(new BigDecimal("15.00")),
                Money.of(new BigDecimal("75.00"))
        );
        testSale = new Sale(
                SALE_ID, LABEL_ID, 10L,
                LocalDate.of(2026, 1, 15),
                ChannelType.DIRECT,
                "Original notes",
                List.of(lineItem),
                Money.of(new BigDecimal("75.00"))
        );

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(testLabel));
        when(saleQueryApi.findById(SALE_ID)).thenReturn(Optional.of(testSale));
        when(releaseQueryApi.getReleasesForLabel(LABEL_ID)).thenReturn(List.of(
                new Release(RELEASE_ID, "Test Release", null, LABEL_ID, List.of(), List.of(),
                        java.util.Set.of())
        ));
    }

    // ── GET edit form ─────────────────────────────────────────────────────────

    @Test
    void showEditForm_returnsEditView() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/sales/{saleId}/edit", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("sale/edit"));
    }

    @Test
    void showEditForm_populatesModelWithLabelAndSale() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/sales/{saleId}/edit", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("label", "sale", "releases", "formats", "form"));
    }

    @Test
    void showEditForm_prePopulatesFormWithExistingSaleData() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/sales/{saleId}/edit", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("form", org.hamcrest.Matchers.hasProperty(
                        "saleDate", org.hamcrest.Matchers.is(LocalDate.of(2026, 1, 15))
                )));
    }

    // ── POST edit form ────────────────────────────────────────────────────────

    @Test
    void submitEdit_redirectsToDetailOnSuccess() throws Exception {
        when(saleCommandApi.updateSale(any(), any(), any(), any())).thenReturn(testSale);

        mockMvc
                .perform(post("/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("saleDate", "2026-01-20")
                        .param("notes", "Updated notes")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "3")
                        .param("lineItems[0].unitPrice", "15.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/" + LABEL_ID + "/sales/" + SALE_ID));
    }

    @Test
    void submitEdit_callsUpdateSaleWithCorrectParameters() throws Exception {
        when(saleCommandApi.updateSale(any(), any(), any(), any())).thenReturn(testSale);

        mockMvc
                .perform(post("/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("saleDate", "2026-01-20")
                        .param("notes", "Updated notes")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "3")
                        .param("lineItems[0].unitPrice", "15.00"))
                .andExpect(status().is3xxRedirection());

        verify(saleCommandApi).updateSale(
                eq(SALE_ID),
                eq(LocalDate.of(2026, 1, 20)),
                eq("Updated notes"),
                any()
        );
    }

    @Test
    void submitEdit_reRendersFormWithErrorOnInsufficientInventory() throws Exception {
        doThrow(new InsufficientInventoryException(999, 0))
                .when(saleCommandApi).updateSale(anyLong(), any(), any(), any());

        mockMvc
                .perform(post("/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("saleDate", "2026-01-20")
                        .param("notes", "")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "999")
                        .param("lineItems[0].unitPrice", "15.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("sale/edit"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // ── POST delete ───────────────────────────────────────────────────────────

    @Test
    void deleteSale_redirectsToSalesListOnSuccess() throws Exception {
        mockMvc
                .perform(post("/labels/{labelId}/sales/{saleId}/delete", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/" + LABEL_ID + "/sales"));
    }

    @Test
    void deleteSale_callsDeleteSaleWithCorrectId() throws Exception {
        mockMvc
                .perform(post("/labels/{labelId}/sales/{saleId}/delete", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(saleCommandApi).deleteSale(SALE_ID);
    }

    @Test
    void deleteSale_redirectsToListEvenWhenSaleNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Sale not found: " + SALE_ID))
                .when(saleCommandApi).deleteSale(anyLong());

        mockMvc
                .perform(post("/labels/{labelId}/sales/{saleId}/delete", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/" + LABEL_ID + "/sales"));
    }
}
