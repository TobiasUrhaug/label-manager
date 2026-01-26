package org.omt.labelmanager.catalog.api.artist;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.domain.artist.ArtistFactory;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.omt.labelmanager.identity.application.AppUserDetails;
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
    private ArtistCRUDHandler artistCRUDHandler;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void artist_returnsArtistView() throws Exception {
        var artist = ArtistFactory.anArtist()
                .id(1L)
                .artistName("DJ Cool")
                .realName(new Person("John Smith"))
                .email("dj@cool.com")
                .build();
        when(artistCRUDHandler.findById(1L)).thenReturn(Optional.of(artist));

        mockMvc
                .perform(get("/artists/1").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("artists/artist"))
                .andExpect(model().attribute("id", 1L))
                .andExpect(model().attribute("artistName", "DJ Cool"))
                .andExpect(model().attribute("email", "dj@cool.com"));
    }

    @Test
    void artist_returns404_whenNotFound() throws Exception {
        when(artistCRUDHandler.findById(999L)).thenReturn(Optional.empty());

        mockMvc
                .perform(get("/artists/999").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createArtist_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(post("/artists")
                        .with(user(testUser))
                        .with(csrf())
                        .param("artistName", "New Artist")
                        .param("realName", "Real Name")
                        .param("email", "artist@email.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(artistCRUDHandler).createArtist(
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
                .perform(post("/artists")
                        .with(user(testUser))
                        .with(csrf())
                        .param("artistName", "New Artist")
                        .param("street", "123 Music Lane")
                        .param("city", "Oslo")
                        .param("postalCode", "0123")
                        .param("country", "Norway"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(artistCRUDHandler).createArtist(
                "New Artist",
                null,
                null,
                new Address("123 Music Lane", null, "Oslo", "0123", "Norway"),
                1L
        );
    }

    @Test
    void updateArtist_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(put("/artists/1")
                        .with(user(testUser))
                        .with(csrf())
                        .param("artistName", "Updated Artist")
                        .param("realName", "New Real Name")
                        .param("email", "updated@email.com")
                        .param("street", "456 New St")
                        .param("street2", "Apt 2")
                        .param("city", "Bergen")
                        .param("postalCode", "5020")
                        .param("country", "Norway"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artists/1"));

        verify(artistCRUDHandler).updateArtist(
                1L,
                "Updated Artist",
                new Person("New Real Name"),
                "updated@email.com",
                new Address("456 New St", "Apt 2", "Bergen", "5020", "Norway")
        );
    }

    @Test
    void deleteArtist_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(delete("/artists/1")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(artistCRUDHandler).delete(1L);
    }
}
