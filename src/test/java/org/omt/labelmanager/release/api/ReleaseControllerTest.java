package org.omt.labelmanager.release.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.artist.ArtistCRUDHandler;
import org.omt.labelmanager.artist.ArtistFactory;
import org.omt.labelmanager.cost.CostQueryService;
import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.label.LabelFactory;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.omt.labelmanager.release.ReleaseFactory;
import org.omt.labelmanager.release.ReleaseFormat;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.omt.labelmanager.track.TrackFactory;
import org.omt.labelmanager.user.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReleaseController.class)
@Import(TestSecurityConfig.class)
class ReleaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelCRUDHandler labelCRUDHandler;

    @MockitoBean
    private ReleaseCRUDHandler releaseCRUDHandler;

    @MockitoBean
    private ArtistCRUDHandler artistCRUDHandler;

    @MockitoBean
    private CostQueryService costQueryService;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void release_returnsReleaseViewAndPopulatedModel() throws Exception {
        var label = LabelFactory.aLabel().id(1L).name("My Label").build();
        var releaseDate = LocalDate.now();
        var artist = ArtistFactory.anArtist().id(1L).artistName("Test Artist").build();
        var anotherArtist = ArtistFactory.anArtist().id(2L).artistName("Another Artist").build();
        var track = TrackFactory.aTrack()
                .artist(artist)
                .name("Test Track")
                .durationSeconds(210)
                .position(1)
                .build();
        var formats = Set.of(ReleaseFormat.DIGITAL, ReleaseFormat.VINYL);
        var release = ReleaseFactory
                .aRelease()
                .id(4L)
                .name("First Release")
                .releaseDate(releaseDate)
                .label(label)
                .artist(artist)
                .tracks(List.of(track))
                .formats(formats)
                .build();

        when(labelCRUDHandler.findById(1L)).thenReturn(Optional.of(label));
        when(releaseCRUDHandler.findById(4L)).thenReturn(Optional.of(release));
        when(artistCRUDHandler.getArtistsForUser(1L)).thenReturn(List.of(artist, anotherArtist));

        mockMvc.perform(get("/labels/1/releases/4").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("/releases/release"))
                .andExpect(model().attribute("name", "First Release"))
                .andExpect(model().attribute("labelId", 1L))
                .andExpect(model().attribute("releaseId", 4L))
                .andExpect(model().attribute("releaseDate", releaseDate))
                .andExpect(model().attribute("artists", List.of(artist)))
                .andExpect(model().attribute("tracks", List.of(track)))
                .andExpect(model().attribute("formats", formats))
                .andExpect(model().attribute("allArtists", List.of(artist, anotherArtist)))
                .andExpect(model().attribute("allFormats", ReleaseFormat.values()));
    }

    @Test
    void release_returns404_whenResourceNotFound() throws Exception {
        when(labelCRUDHandler.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRelease_callsHandlerAndRedirectsToLabel() throws Exception {
        mockMvc
                .perform(delete("/labels/1/releases/5")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(releaseCRUDHandler).delete(5L);
    }

    @Test
    void updateRelease_callsHandlerAndRedirectsToRelease() throws Exception {
        mockMvc
                .perform(put("/labels/1/releases/5")
                        .with(user(testUser))
                        .with(csrf())
                        .param("releaseName", "Updated Release")
                        .param("releaseDate", "2026-06-15")
                        .param("artistIds", "1", "2")
                        .param("tracks[0].name", "Track 1")
                        .param("tracks[0].duration", "3:30")
                        .param("tracks[0].artistIds", "1")
                        .param("formats", "VINYL", "CD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/5"));

        verify(releaseCRUDHandler).updateRelease(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq("Updated Release"),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 6, 15)),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anySet()
        );
    }

}
