package org.omt.labelmanager.infrastructure.web.dashboard;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.ArtistFactory;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.dashboard.DashboardController;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(TestSecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelQueryApi labelQueryFacade;

    @MockitoBean
    private ArtistQueryApi artistQueryApi;

    @Test
    void dashboard_returnsLabels() throws Exception {
        var testUser = new AppUserDetails(1L, "test@example.com", "password", "Test User");
        var labelA = LabelFactory.aLabel().id(1L).name("My Label").build();
        var labelB = LabelFactory.aLabel().id(2L).name("Other Label").build();
        when(labelQueryFacade.getLabelsForUser(1L)).thenReturn(List.of(labelA, labelB));
        when(artistQueryApi.getArtistsForUser(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels").isArray())
                .andExpect(jsonPath("$.labels[0].name").value("My Label"))
                .andExpect(jsonPath("$.labels[1].name").value("Other Label"));
    }

    @Test
    void dashboard_returnsArtists() throws Exception {
        var testUser = new AppUserDetails(1L, "test@example.com", "password", "Test User");
        var artistA = ArtistFactory.anArtist().id(1L).artistName("Artist A").build();
        var artistB = ArtistFactory.anArtist().id(2L).artistName("Artist B").build();
        when(labelQueryFacade.getLabelsForUser(1L)).thenReturn(List.of());
        when(artistQueryApi.getArtistsForUser(1L)).thenReturn(List.of(artistA, artistB));

        mockMvc.perform(get("/api/dashboard").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artists").isArray())
                .andExpect(jsonPath("$.artists[0].artistName").value("Artist A"))
                .andExpect(jsonPath("$.artists[1].artistName").value("Artist B"));
    }

}
