package org.omt.labelmanager.sales.distributor_return.api;

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
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.omt.labelmanager.identity.application.AppUserDetails;
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

    private Label testLabel;
    private DistributorReturn testReturn;
    private Distributor testDistributor;

    @BeforeEach
    void setUp() {
        testLabel = new Label(LABEL_ID, "Test Label", null, null, null, null, 1L);

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
        when(releaseQueryApi.getReleasesForLabel(LABEL_ID)).thenReturn(List.of(
                new Release(RELEASE_ID, "Test Release", null, LABEL_ID, List.of(), List.of(),
                        java.util.Set.of())
        ));
        when(distributorQueryApi.findByLabelId(LABEL_ID)).thenReturn(List.of(testDistributor));
        when(releaseQueryApi.findById(RELEASE_ID)).thenReturn(
                Optional.of(new Release(RELEASE_ID, "Test Release", null, LABEL_ID,
                        List.of(), List.of(), java.util.Set.of()))
        );
    }

    // ── GET list ──────────────────────────────────────────────────────────────

    @Test
    void listReturns_returnsListView() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("return/list"));
    }

    @Test
    void listReturns_populatesModelWithLabelAndReturns() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("label", "returns", "distributors"));
    }

    // ── GET register form ─────────────────────────────────────────────────────

    @Test
    void showRegisterForm_returnsRegisterView() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns/new", LABEL_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("return/register"));
    }

    @Test
    void showRegisterForm_populatesModelWithRequiredAttributes() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns/new", LABEL_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(
                        "label", "releases", "distributors", "formats", "form"));
    }

    // ── POST register ─────────────────────────────────────────────────────────

    @Test
    void registerReturn_redirectsToListOnSuccess() throws Exception {
        when(returnCommandApi.registerReturn(any(), any(), any(), any(), any()))
                .thenReturn(testReturn);

        mockMvc
                .perform(post("/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("distributorId", DISTRIBUTOR_ID.toString())
                        .param("returnDate", "2026-01-15")
                        .param("notes", "Test notes")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/" + LABEL_ID + "/returns"));
    }

    @Test
    void registerReturn_callsCommandApiWithCorrectParameters() throws Exception {
        when(returnCommandApi.registerReturn(any(), any(), any(), any(), any()))
                .thenReturn(testReturn);

        mockMvc
                .perform(post("/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("distributorId", DISTRIBUTOR_ID.toString())
                        .param("returnDate", "2026-01-15")
                        .param("notes", "Test notes")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "5"))
                .andExpect(status().is3xxRedirection());

        verify(returnCommandApi).registerReturn(
                eq(LABEL_ID),
                eq(DISTRIBUTOR_ID),
                eq(LocalDate.of(2026, 1, 15)),
                eq("Test notes"),
                any()
        );
    }

    @Test
    void registerReturn_reRendersFormWithErrorOnInsufficientInventory() throws Exception {
        doThrow(new IllegalStateException("Insufficient inventory"))
                .when(returnCommandApi).registerReturn(any(), any(), any(), any(), any());

        mockMvc
                .perform(post("/labels/{labelId}/returns", LABEL_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("distributorId", DISTRIBUTOR_ID.toString())
                        .param("returnDate", "2026-01-15")
                        .param("notes", "")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("return/register"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // ── GET detail ────────────────────────────────────────────────────────────

    @Test
    void viewReturn_returnsDetailView() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("return/detail"));
    }

    @Test
    void viewReturn_populatesModelWithReturnAndDistributor() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(
                        "label", "distributorReturn", "distributor", "releaseNames"));
    }

    // ── GET edit form ─────────────────────────────────────────────────────────

    @Test
    void showEditForm_returnsEditView() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns/{returnId}/edit", LABEL_ID, RETURN_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("return/edit"));
    }

    @Test
    void showEditForm_populatesModelWithRequiredAttributes() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns/{returnId}/edit", LABEL_ID, RETURN_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists(
                        "label", "distributorReturn", "releases", "distributors", "formats",
                        "form"));
    }

    @Test
    void showEditForm_prePopulatesFormWithExistingReturnDate() throws Exception {
        mockMvc
                .perform(get("/labels/{labelId}/returns/{returnId}/edit", LABEL_ID, RETURN_ID)
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("form", org.hamcrest.Matchers.hasProperty(
                        "returnDate", org.hamcrest.Matchers.is(LocalDate.of(2026, 1, 15))
                )));
    }

    // ── POST edit ─────────────────────────────────────────────────────────────

    @Test
    void submitEdit_redirectsToDetailOnSuccess() throws Exception {
        mockMvc
                .perform(post("/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("returnDate", "2026-01-20")
                        .param("notes", "Updated notes")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/" + LABEL_ID + "/returns/" + RETURN_ID));
    }

    @Test
    void submitEdit_callsUpdateReturnWithCorrectParameters() throws Exception {
        mockMvc
                .perform(post("/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("returnDate", "2026-01-20")
                        .param("notes", "Updated notes")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "3"))
                .andExpect(status().is3xxRedirection());

        verify(returnCommandApi).updateReturn(
                eq(RETURN_ID),
                eq(LocalDate.of(2026, 1, 20)),
                eq("Updated notes"),
                any()
        );
    }

    @Test
    void submitEdit_reRendersFormWithErrorOnInsufficientInventory() throws Exception {
        doThrow(new IllegalStateException("Insufficient inventory"))
                .when(returnCommandApi).updateReturn(anyLong(), any(), any(), any());

        mockMvc
                .perform(post("/labels/{labelId}/returns/{returnId}", LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .param("returnDate", "2026-01-20")
                        .param("notes", "")
                        .param("lineItems[0].releaseId", RELEASE_ID.toString())
                        .param("lineItems[0].format", "VINYL")
                        .param("lineItems[0].quantity", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("return/edit"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // ── POST delete ───────────────────────────────────────────────────────────

    @Test
    void deleteReturn_redirectsToReturnListOnSuccess() throws Exception {
        mockMvc
                .perform(post("/labels/{labelId}/returns/{returnId}/delete",
                        LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/" + LABEL_ID + "/returns"));
    }

    @Test
    void deleteReturn_callsDeleteReturnWithCorrectId() throws Exception {
        mockMvc
                .perform(post("/labels/{labelId}/returns/{returnId}/delete",
                        LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(returnCommandApi).deleteReturn(RETURN_ID);
    }

    @Test
    void deleteReturn_redirectsToListEvenWhenReturnNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Return not found: " + RETURN_ID))
                .when(returnCommandApi).deleteReturn(anyLong());

        mockMvc
                .perform(post("/labels/{labelId}/returns/{returnId}/delete",
                        LABEL_ID, RETURN_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/" + LABEL_ID + "/returns"));
    }
}
