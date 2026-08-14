package org.omt.labelmanager.inventory.productionrun.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.ReleaseFactory;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRunFactory;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LabelProductionRunController.class)
@Import(TestSecurityConfig.class)
class LabelProductionRunControllerTest {

    private static final Long LABEL_ID = 1L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReleaseQueryApi releaseQueryApi;

    @MockitoBean private ProductionRunQueryApi productionRunQueryApi;

    @MockitoBean private LabelQueryApi labelQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @BeforeEach
    void labelExists() {
        when(labelQueryApi.exists(anyLong())).thenReturn(true);
    }

    @Test
    void productionRuns_returnsRunsAcrossEveryReleaseOfTheLabel() throws Exception {
        when(releaseQueryApi.getReleasesForLabel(LABEL_ID))
                .thenReturn(
                        List.of(
                                ReleaseFactory.aRelease().id(4L).labelId(LABEL_ID).build(),
                                ReleaseFactory.aRelease().id(5L).labelId(LABEL_ID).build()));
        when(productionRunQueryApi.findByReleaseIds(List.of(4L, 5L)))
                .thenReturn(
                        List.of(
                                ProductionRunFactory.aProductionRun().id(10L).releaseId(4L).build(),
                                ProductionRunFactory.aProductionRun()
                                        .id(11L)
                                        .releaseId(5L)
                                        .build()));

        mockMvc.perform(get("/api/labels/1/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[1].id").value(11));

        // One batch query, not one per release — the reason findByReleaseIds exists.
        verify(productionRunQueryApi).findByReleaseIds(List.of(4L, 5L));
    }

    @Test
    void productionRuns_returnsEmptyWhenTheLabelHasNoReleases() throws Exception {
        when(releaseQueryApi.getReleasesForLabel(LABEL_ID)).thenReturn(List.of());
        when(productionRunQueryApi.findByReleaseIds(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/labels/1/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void productionRuns_returns404WhenTheLabelDoesNotExist() throws Exception {
        when(labelQueryApi.exists(999L)).thenReturn(false);

        mockMvc.perform(get("/api/labels/999/production-runs").with(user(testUser)))
                .andExpect(status().isNotFound());

        verify(productionRunQueryApi, org.mockito.Mockito.never())
                .findByReleaseIds(org.mockito.ArgumentMatchers.any());
    }
}
