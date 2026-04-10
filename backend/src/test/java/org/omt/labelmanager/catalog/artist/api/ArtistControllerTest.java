package org.omt.labelmanager.catalog.artist.api;

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

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.artist.domain.ArtistFactory;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArtistController.class)
@Import(TestSecurityConfig.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistCommandApi artistCommandApi;

    @MockitoBean
    private ArtistQueryApi artistQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void artist_returnsArtistJson() throws Exception {
        var artist = ArtistFactory.anArtist()
                .id(1L)
                .artistName("DJ Cool")
                .realName(new Person("John Smith"))
                .email("dj@cool.com")
                .build();
        when(artistQueryApi.findById(1L)).thenReturn(Optional.of(artist));

        mockMvc
                .perform(get("/api/artists/1").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artistName").value("DJ Cool"))
                .andExpect(jsonPath("$.email").value("dj@cool.com"));
    }

    @Test
    void artist_returns404_whenNotFound() throws Exception {
        when(artistQueryApi.findById(999L)).thenReturn(Optional.empty());

        mockMvc
                .perform(get("/api/artists/999").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createArtist_returnsCreated() throws Exception {
        mockMvc
                .perform(post("/api/artists")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "artistName": "New Artist",
                                  "realName": "Real Name",
                                  "email": "artist@email.com"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(artistCommandApi).createArtist(
                "New Artist",
                new Person("Real Name"),
                "artist@email.com",
                null,
                1L
        );
    }

    @Test
    void createArtist_withAddress() throws Exception {
        mockMvc
                .perform(post("/api/artists")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "artistName": "New Artist",
                                  "street": "123 Music Lane",
                                  "city": "Oslo",
                                  "postalCode": "0123",
                                  "country": "Norway"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(artistCommandApi).createArtist(
                "New Artist",
                null,
                null,
                new Address("123 Music Lane", null, "Oslo", "0123", "Norway"),
                1L
        );
    }

    @Test
    void updateArtist_returnsNoContent() throws Exception {
        mockMvc
                .perform(put("/api/artists/1")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "artistName": "Updated Artist",
                                  "realName": "New Real Name",
                                  "email": "updated@email.com",
                                  "street": "456 New St",
                                  "street2": "Apt 2",
                                  "city": "Bergen",
                                  "postalCode": "5020",
                                  "country": "Norway"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(artistCommandApi).updateArtist(
                1L,
                "Updated Artist",
                new Person("New Real Name"),
                "updated@email.com",
                new Address("456 New St", "Apt 2", "Bergen", "5020", "Norway")
        );
    }

    @Test
    void deleteArtist_returnsNoContent() throws Exception {
        mockMvc
                .perform(delete("/api/artists/1")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(artistCommandApi).delete(1L);
    }
}
