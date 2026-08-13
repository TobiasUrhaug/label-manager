package org.omt.labelmanager.infrastructure.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.api.SaleController;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that exceptions crossing a controller boundary are rendered as RFC 9457
 * ProblemDetail responses. SaleController is used as the vehicle because it throws
 * EntityNotFoundException from a plain GET; the behaviour under test is the advice.
 */
@WebMvcTest(SaleController.class)
@Import(TestSecurityConfig.class)
class ApiExceptionHandlerTest {

    private static final Long LABEL_ID = 1L;
    private static final Long MISSING_SALE_ID = 404L;

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

    @Test
    void entityNotFound_rendersProblemDetailWith404() throws Exception {
        when(labelQueryApi.findById(LABEL_ID))
                .thenReturn(Optional.of(
                        new org.omt.labelmanager.catalog.label.domain.Label(
                                LABEL_ID, "Test Label", null, null, null, null, 1L)));
        when(saleQueryApi.findById(MISSING_SALE_ID)).thenReturn(Optional.empty());

        mockMvc
                .perform(get("/api/labels/{labelId}/sales/{saleId}", LABEL_ID, MISSING_SALE_ID)
                        .with(user(testUser)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Sale not found"));
    }
}
