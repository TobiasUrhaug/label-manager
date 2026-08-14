package org.omt.labelmanager.web.catalog;

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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.ArtistFactory;
import org.omt.labelmanager.catalog.release.ReleaseFactory;
import org.omt.labelmanager.catalog.release.TrackFactory;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.shared.Format;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReleaseController.class)
@Import(TestSecurityConfig.class)
class ReleaseControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ReleaseCommandApi releaseCommandFacade;

    @MockitoBean private ReleaseQueryApi releaseQueryFacade;

    @MockitoBean private ArtistQueryApi artistQueryApi;

    @MockitoBean private CostQueryApi costQueryFacade;

    @MockitoBean private ProductionRunQueryApi productionRunQueryService;

    @MockitoBean private DistributorQueryApi distributorQueryService;

    @MockitoBean private InventoryMovementQueryApi inventoryMovementQueryApi;

    @MockitoBean private SaleQueryApi saleQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void releases_returnsTheLabelsReleases() throws Exception {
        var release = ReleaseFactory.aRelease().id(4L).name("First Release").labelId(1L).build();
        when(releaseQueryFacade.getReleasesForLabel(1L)).thenReturn(List.of(release));

        mockMvc.perform(get("/api/labels/1/releases").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("First Release"));
    }

    @Test
    void release_returnsReleaseJson() throws Exception {
        var releaseDate = LocalDate.of(2026, 3, 15);
        var artist = ArtistFactory.anArtist().id(1L).artistName("Test Artist").build();
        var track =
                TrackFactory.aTrack()
                        .artistId(1L)
                        .name("Test Track")
                        .durationSeconds(210)
                        .position(1)
                        .build();
        var formats = Set.of(Format.DIGITAL, Format.VINYL);
        var release =
                ReleaseFactory.aRelease()
                        .id(4L)
                        .name("First Release")
                        .releaseDate(releaseDate)
                        .labelId(1L)
                        .artistId(1L)
                        .tracks(List.of(track))
                        .formats(formats)
                        .build();

        when(releaseQueryFacade.findById(4L)).thenReturn(Optional.of(release));
        when(artistQueryApi.getArtistsForUser(1L)).thenReturn(List.of(artist));

        mockMvc.perform(get("/api/labels/1/releases/4").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("First Release"))
                .andExpect(jsonPath("$.artists").isArray())
                .andExpect(jsonPath("$.tracks").isArray())
                .andExpect(jsonPath("$.tracks[0].name").value("Test Track"));
    }

    @Test
    void release_doesNotBundleCostsRunsDistributorsOrSales() throws Exception {
        var release = ReleaseFactory.aRelease().id(4L).labelId(1L).build();
        when(releaseQueryFacade.findById(4L)).thenReturn(Optional.of(release));
        when(artistQueryApi.getArtistsForUser(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/labels/1/releases/4").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costs").doesNotExist())
                .andExpect(jsonPath("$.productionRuns").doesNotExist())
                .andExpect(jsonPath("$.distributors").doesNotExist())
                .andExpect(jsonPath("$.releaseSales").doesNotExist())
                .andExpect(jsonPath("$.totalUnitsSold").doesNotExist());
    }

    @Test
    void release_returns404_whenNotFound() throws Exception {
        when(releaseQueryFacade.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labels/1/releases/999").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRelease_returnsCreated() throws Exception {
        mockMvc.perform(
                        post("/api/labels/1/releases")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "releaseName": "New Release",
                                  "releaseDate": "2026-06-15",
                                  "artistIds": [1, 2],
                                  "tracks": [],
                                  "formats": ["VINYL"]
                                }
                                """))
                .andExpect(status().isCreated());

        verify(releaseCommandFacade)
                .createRelease(
                        org.mockito.ArgumentMatchers.eq("New Release"),
                        org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 6, 15)),
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anySet());
    }

    @Test
    void updateRelease_returnsNoContent() throws Exception {
        mockMvc.perform(
                        put("/api/labels/1/releases/5")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "releaseName": "Updated Release",
                                  "releaseDate": "2026-06-15",
                                  "artistIds": [1, 2],
                                  "tracks": [],
                                  "formats": ["VINYL", "CD"]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(releaseCommandFacade)
                .updateRelease(
                        org.mockito.ArgumentMatchers.eq(5L),
                        org.mockito.ArgumentMatchers.eq("Updated Release"),
                        org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 6, 15)),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anySet());
    }

    @Test
    void deleteRelease_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/labels/1/releases/5").with(user(testUser)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(releaseCommandFacade).delete(5L);
    }
}
