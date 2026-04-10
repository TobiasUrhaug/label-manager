package org.omt.labelmanager.sales.sale.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.ChannelType;
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
                ReleaseFormat.VINYL,
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
        when(releaseQueryApi.findById(RELEASE_ID)).thenReturn(Optional.of(
                new Release(RELEASE_ID, "Test Release", null, LABEL_ID, List.of(), List.of(),
                        java.util.Set.of())
        ));
    }

    // ── GET list ──────────────────────────────────────────────────────────────

    @Test
    void listSales_returnsOkWithSalesAndTotalRevenue() throws Exception {
        when(saleQueryApi.getSalesForLabel(LABEL_ID)).thenReturn(List.of(testSale));
        when(saleQueryApi.getTotalRevenueForLabel(LABEL_ID))
                .thenReturn(Money.of(new BigDecimal("75.00")));

        mockMvc.perform(get("/api/labels/{labelId}/sales", LABEL_ID).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales").isArray())
                .andExpect(jsonPath("$.sales[0].id").value(SALE_ID.intValue()))
                .andExpect(jsonPath("$.totalRevenue.amount").value(75.00));
    }

    // ── POST register ─────────────────────────────────────────────────────────

    @Test
    void registerSale_returnsCreated() throws Exception {
        when(saleCommandApi.registerSale(any(), any(), any(), any(), any(), any()))
                .thenReturn(testSale);

        mockMvc.perform(post("/api/labels/{labelId}/sales", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "saleDate": "2026-01-15",
                                  "channel": "DIRECT",
                                  "distributorId": null,
                                  "notes": "Test notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 5, "unitPrice": 15.00}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void registerSale_callsCommandWithCorrectParameters() throws Exception {
        when(saleCommandApi.registerSale(any(), any(), any(), any(), any(), any()))
                .thenReturn(testSale);

        mockMvc.perform(post("/api/labels/{labelId}/sales", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "saleDate": "2026-01-15",
                                  "channel": "DIRECT",
                                  "distributorId": null,
                                  "notes": "Test notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 5, "unitPrice": 15.00}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());

        verify(saleCommandApi).registerSale(
                eq(LABEL_ID),
                eq(LocalDate.of(2026, 1, 15)),
                eq(ChannelType.DIRECT),
                eq("Test notes"),
                eq(null),
                any()
        );
    }

    @Test
    void registerSale_returnsBadRequest_onInsufficientInventory() throws Exception {
        doThrow(new InsufficientInventoryException(999, 0))
                .when(saleCommandApi).registerSale(any(), any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/labels/{labelId}/sales", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "saleDate": "2026-01-15",
                                  "channel": "DIRECT",
                                  "distributorId": null,
                                  "notes": "",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 999, "unitPrice": 15.00}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── GET detail ────────────────────────────────────────────────────────────

    @Test
    void viewSale_returnsOkWithEnrichedLineItems() throws Exception {
        mockMvc.perform(get("/api/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SALE_ID.intValue()))
                .andExpect(jsonPath("$.saleDate").value("2026-01-15"))
                .andExpect(jsonPath("$.lineItems").isArray())
                .andExpect(jsonPath("$.lineItems[0].releaseName").value("Test Release"))
                .andExpect(jsonPath("$.lineItems[0].quantity").value(5));
    }

    // ── PUT update ────────────────────────────────────────────────────────────

    @Test
    void updateSale_returnsOkWithUpdatedSale() throws Exception {
        when(saleCommandApi.updateSale(any(), any(), any(), any())).thenReturn(testSale);

        mockMvc.perform(put("/api/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "saleDate": "2026-01-20",
                                  "notes": "Updated notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 3, "unitPrice": 15.00}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SALE_ID.intValue()));
    }

    @Test
    void updateSale_callsCommandWithCorrectParameters() throws Exception {
        when(saleCommandApi.updateSale(any(), any(), any(), any())).thenReturn(testSale);

        mockMvc.perform(put("/api/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "saleDate": "2026-01-20",
                                  "notes": "Updated notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 3, "unitPrice": 15.00}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        verify(saleCommandApi).updateSale(
                eq(SALE_ID),
                eq(LocalDate.of(2026, 1, 20)),
                eq("Updated notes"),
                any()
        );
    }

    @Test
    void updateSale_returnsBadRequest_onInsufficientInventory() throws Exception {
        doThrow(new InsufficientInventoryException(999, 0))
                .when(saleCommandApi).updateSale(anyLong(), any(), any(), any());

        mockMvc.perform(put("/api/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "saleDate": "2026-01-20",
                                  "notes": "",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 999, "unitPrice": 15.00}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteSale_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSale_callsDeleteSaleWithCorrectId() throws Exception {
        mockMvc.perform(delete("/api/labels/{labelId}/sales/{saleId}", LABEL_ID, SALE_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(saleCommandApi).deleteSale(SALE_ID);
    }
}
