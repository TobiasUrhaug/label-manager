package org.omt.labelmanager.catalog.label.api;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.ArtistFactory;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.release.ReleaseFactory;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.DistributorFactory;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabelController.class)
@Import(TestSecurityConfig.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelCommandApi labelCommandFacade;

    @MockitoBean
    private LabelQueryApi labelQueryFacade;

    @MockitoBean
    private ReleaseQueryApi releaseQueryFacade;

    @MockitoBean
    private ArtistQueryApi artistQueryApi;

    @MockitoBean
    private DistributorQueryApi distributorQueryService;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void label_returnsLabelJson() throws Exception {
        var label = LabelFactory.aLabel()
                .id(1L)
                .name("My Label")
                .email("contact@mylabel.com")
                .website("https://mylabel.com")
                .build();
        when(labelQueryFacade.findById(1L)).thenReturn(Optional.of(label));

        var release = ReleaseFactory.aRelease().id(1L).name("My Release").build();
        when(releaseQueryFacade.getReleasesForLabel(1L)).thenReturn(List.of(release));

        var artist = ArtistFactory.anArtist().id(1L).artistName("Unknown").build();
        when(artistQueryApi.getArtistsForUser(1L)).thenReturn(List.of(artist));

        var distributor = DistributorFactory.aDistributor().id(1L).name("Direct Sales").build();
        when(distributorQueryService.findByLabelId(1L)).thenReturn(List.of(distributor));

        mockMvc
                .perform(get("/api/labels/1").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Label"))
                .andExpect(jsonPath("$.email").value("contact@mylabel.com"))
                .andExpect(jsonPath("$.website").value("https://mylabel.com"))
                .andExpect(jsonPath("$.releases").isArray())
                .andExpect(jsonPath("$.artists").isArray())
                .andExpect(jsonPath("$.distributors").isArray());
    }

    @Test
    void label_returns404_whenResourceNotFound() throws Exception {
        when(labelQueryFacade.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labels/1123").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createLabel_returnsCreated() throws Exception {
        mockMvc
                .perform(post("/api/labels")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "labelName": "New Label",
                                  "email": "info@newlabel.com",
                                  "website": "https://newlabel.com"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(labelCommandFacade).createLabel(
                "New Label", "info@newlabel.com", "https://newlabel.com", null, null, 1L);
    }

    @Test
    void updateLabel_returnsNoContent() throws Exception {
        mockMvc
                .perform(put("/api/labels/1")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "labelName": "Updated Label",
                                  "email": "updated@label.com",
                                  "website": "https://updated.com",
                                  "ownerName": "New Owner",
                                  "street": "456 New St",
                                  "street2": "Suite 100",
                                  "city": "Bergen",
                                  "postalCode": "5020",
                                  "country": "Norway"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(labelCommandFacade).updateLabel(
                1L,
                "Updated Label",
                "updated@label.com",
                "https://updated.com",
                new Address("456 New St", "Suite 100", "Bergen", "5020", "Norway"),
                new Person("New Owner")
        );
    }

    @Test
    void deleteLabel_returnsNoContent() throws Exception {
        mockMvc
                .perform(delete("/api/labels/1")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(labelCommandFacade).delete(1L);
    }
}
