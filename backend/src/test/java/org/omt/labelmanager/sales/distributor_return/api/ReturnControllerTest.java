package org.omt.labelmanager.sales.distributor_return.api;

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

import java.time.Instant;
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
import org.omt.labelmanager.distribution.distributor.Distributor;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItem;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReturnController.class)
@Import(TestSecurityConfig.class)
class ReturnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DistributorReturnCommandApi returnCommandApi;

    @MockitoBean
    private DistributorReturnQueryApi returnQueryApi;

    @MockitoBean
    private LabelQueryApi labelQueryApi;

    @MockitoBean
    private ReleaseQueryApi releaseQueryApi;

    @MockitoBean
    private DistributorQueryApi distributorQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    private static final Long LABEL_ID = 1L;
    private static final Long RETURN_ID = 42L;
    private static final Long RELEASE_ID = 10L;
    private static final Long DISTRIBUTOR_ID = 5L;

    private DistributorReturn testReturn;
    private Distributor testDistributor;

    @BeforeEach
    void setUp() {
        var testLabel = new Label(LABEL_ID, "Test Label", null, null, null, null, 1L);
        var lineItem = new ReturnLineItem(1L, RETURN_ID, RELEASE_ID, ReleaseFormat.VINYL, 5);
        testReturn = new DistributorReturn(
                RETURN_ID, LABEL_ID, DISTRIBUTOR_ID,
                LocalDate.of(2026, 1, 15),
                "Original notes",
                List.of(lineItem),
                Instant.now()
        );
        testDistributor = new Distributor(DISTRIBUTOR_ID, LABEL_ID, "Test Distributor",
                ChannelType.DISTRIBUTOR);

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(testLabel));
        when(returnQueryApi.findById(RETURN_ID)).thenReturn(Optional.of(testReturn));
        when(returnQueryApi.getReturnsForLabel(LABEL_ID)).thenReturn(List.of(testReturn));
        when(distributorQueryApi.findByLabelId(LABEL_ID)).thenReturn(List.of(testDistributor));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(testDistributor));
        when(releaseQueryApi.findById(RELEASE_ID)).thenReturn(Optional.of(
                new Release(RELEASE_ID, "Test Release", null, LABEL_ID,
                        List.of(), List.of(), java.util.Set.of())
        ));
    }

    // ── GET list ──────────────────────────────────────────────────────────────

    @Test
    void listReturns_returnsOkWithReturnsAndDistributors() throws Exception {
        mockMvc.perform(get("/api/labels/{labelId}/returns", LABEL_ID).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returns").isArray())
                .andExpect(jsonPath("$.returns[0].id").value(RETURN_ID.intValue()))
                .andExpect(jsonPath("$.distributors").isArray())
                .andExpect(jsonPath("$.distributors[0].name").value("Test Distributor"));
    }

    // ── POST register ─────────────────────────────────────────────────────────

    @Test
    void registerReturn_returnsCreated() throws Exception {
        when(returnCommandApi.registerReturn(any(), any(), any(), any(), any()))
                .thenReturn(testReturn);

        mockMvc.perform(post("/api/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "distributorId": 5,
                                  "returnDate": "2026-01-15",
                                  "notes": "Test notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 5}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void registerReturn_callsCommandWithCorrectParameters() throws Exception {
        when(returnCommandApi.registerReturn(any(), any(), any(), any(), any()))
                .thenReturn(testReturn);

        mockMvc.perform(post("/api/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "distributorId": 5,
                                  "returnDate": "2026-01-15",
                                  "notes": "Test notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 5}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated());

        verify(returnCommandApi).registerReturn(
                eq(LABEL_ID),
                eq(DISTRIBUTOR_ID),
                eq(LocalDate.of(2026, 1, 15)),
                eq("Test notes"),
                any()
        );
    }

    @Test
    void registerReturn_returnsBadRequest_onInsufficientInventory() throws Exception {
        doThrow(new InsufficientInventoryException(999, 0))
                .when(returnCommandApi).registerReturn(any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "distributorId": 5,
                                  "returnDate": "2026-01-15",
                                  "notes": "",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 999}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── GET detail ────────────────────────────────────────────────────────────

    @Test
    void viewReturn_returnsOkWithEnrichedLineItemsAndDistributor() throws Exception {
        mockMvc.perform(get("/api/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RETURN_ID.intValue()))
                .andExpect(jsonPath("$.returnDate").value("2026-01-15"))
                .andExpect(jsonPath("$.distributor.name").value("Test Distributor"))
                .andExpect(jsonPath("$.lineItems").isArray())
                .andExpect(jsonPath("$.lineItems[0].releaseName").value("Test Release"))
                .andExpect(jsonPath("$.lineItems[0].quantity").value(5));
    }

    // ── PUT update ────────────────────────────────────────────────────────────

    @Test
    void updateReturn_returnsOkWithUpdatedReturn() throws Exception {
        mockMvc.perform(put("/api/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "returnDate": "2026-01-20",
                                  "notes": "Updated notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 3}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RETURN_ID.intValue()));
    }

    @Test
    void updateReturn_callsCommandWithCorrectParameters() throws Exception {
        mockMvc.perform(put("/api/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "returnDate": "2026-01-20",
                                  "notes": "Updated notes",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 3}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        verify(returnCommandApi).updateReturn(
                eq(RETURN_ID),
                eq(LocalDate.of(2026, 1, 20)),
                eq("Updated notes"),
                any()
        );
    }

    @Test
    void updateReturn_returnsBadRequest_onInsufficientInventory() throws Exception {
        doThrow(new InsufficientInventoryException(999, 0))
                .when(returnCommandApi).updateReturn(anyLong(), any(), any(), any());

        mockMvc.perform(put("/api/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "returnDate": "2026-01-20",
                                  "notes": "",
                                  "lineItems": [
                                    {"releaseId": 10, "format": "VINYL", "quantity": 999}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteReturn_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturn_callsDeleteReturnWithCorrectId() throws Exception {
        mockMvc.perform(delete("/api/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(returnCommandApi).deleteReturn(RETURN_ID);
    }
}
