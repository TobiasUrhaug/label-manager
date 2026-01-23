package org.omt.labelmanager.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.artist.ArtistCRUDHandler;
import org.omt.labelmanager.artist.ArtistFactory;
import org.omt.labelmanager.dashboard.api.DashboardController;
import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.label.LabelFactory;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelCRUDHandler labelCRUDHandler;

    @MockitoBean
    private ArtistCRUDHandler artistCRUDHandler;

    @MockitoBean
    private ReleaseCRUDHandler releaseCRUDHandler;

    @Test
    void dashboard_greetsUser() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("user", "Alex The Manager"));
    }

    @Test
    void dashboard_showsListOfLabels() throws Exception {
        var labelA = LabelFactory.aLabel().id(1L).name("My Label").build();
        var labelB = LabelFactory.aLabel().id(1L).name("My Label").build();
        when(labelCRUDHandler.getAllLabels()).thenReturn(List.of(labelA, labelB));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("labels", List.of(labelA, labelB)));
    }

    @Test
    void dashboard_showsListOfArtists() throws Exception {
        var artistA = ArtistFactory.anArtist().id(1L).artistName("Artist A").build();
        var artistB = ArtistFactory.anArtist().id(2L).artistName("Artist B").build();
        when(artistCRUDHandler.getAllArtists()).thenReturn(List.of(artistA, artistB));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("artists", List.of(artistA, artistB)));
    }

}