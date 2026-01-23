package org.omt.labelmanager.release.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.artist.ArtistFactory;
import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.label.LabelFactory;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.omt.labelmanager.release.ReleaseFactory;
import org.omt.labelmanager.track.Track;
import org.omt.labelmanager.track.TrackFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReleaseController.class)
class ReleaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelCRUDHandler labelCRUDHandler;

    @MockitoBean
    private ReleaseCRUDHandler releaseCRUDHandler;

    @Test
    void release_returnsReleaseViewAndPopulatedModel() throws Exception {
        var label = LabelFactory.aLabel().id(1L).name("My Label").build();
        var releaseDate = LocalDate.now();
        var artist = ArtistFactory.anArtist().artistName("Test Artist").build();
        var track = TrackFactory.aTrack()
                .artist(artist)
                .name("Test Track")
                .durationSeconds(210)
                .position(1)
                .build();
        var release = ReleaseFactory
                .aRelease()
                .id(4L)
                .name("First Release")
                .releaseDate(releaseDate)
                .label(label)
                .artist(artist)
                .tracks(List.of(track))
                .build();

        when(labelCRUDHandler.findById(1L)).thenReturn(Optional.of(label));
        when(releaseCRUDHandler.findById(4L)).thenReturn(Optional.of(release));

        mockMvc.perform(get("/labels/1/releases/4"))
                .andExpect(status().isOk())
                .andExpect(view().name("/releases/release"))
                .andExpect(model().attribute("name", "First Release"))
                .andExpect(model().attribute("labelId", 1L))
                .andExpect(model().attribute("releaseId", 4L))
                .andExpect(model().attribute("releaseDate", releaseDate))
                .andExpect(model().attribute("artists", List.of(artist)))
                .andExpect(model().attribute("tracks", List.of(track)));
    }

    @Test
    void release_returns404_whenResourceNotFound() throws Exception {
        when(labelCRUDHandler.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRelease_callsHandlerAndRedirectsToLabel() throws Exception {
        mockMvc
                .perform(delete("/labels/1/releases/5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(releaseCRUDHandler).delete(5L);
    }

}