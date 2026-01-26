package org.omt.labelmanager.catalog.api.label;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.domain.artist.ArtistFactory;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.application.LabelCRUDHandler;
import org.omt.labelmanager.catalog.domain.label.LabelFactory;
import org.omt.labelmanager.catalog.application.ReleaseCRUDHandler;
import org.omt.labelmanager.catalog.domain.release.ReleaseFactory;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.omt.labelmanager.identity.user.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LabelController.class)
@Import(TestSecurityConfig.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelCRUDHandler labelCRUDHandler;

    @MockitoBean
    private ReleaseCRUDHandler releaseCRUDHandler;

    @MockitoBean
    private ArtistCRUDHandler artistCRUDHandler;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void label_redirectsToALabel() throws Exception {
        var label = LabelFactory.aLabel()
                .id(1L)
                .name("My Label")
                .email("contact@mylabel.com")
                .website("https://mylabel.com")
                .build();
        when(labelCRUDHandler.findById(1L)).thenReturn(Optional.of(label));

        var release = ReleaseFactory.aRelease().id(1L).name("My Release").build();
        when(releaseCRUDHandler.getReleasesForLabel(1L)).thenReturn(List.of(release));

        var artist = ArtistFactory.anArtist().id(1L).artistName("Unknown").build();
        when(artistCRUDHandler.getArtistsForUser(1L)).thenReturn(List.of(artist));

        mockMvc
                .perform(get("/labels/1").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("labels/label"))
                .andExpect(model().attribute("name", "My Label"))
                .andExpect(model().attribute("id", 1L))
                .andExpect(model().attribute("email", "contact@mylabel.com"))
                .andExpect(model().attribute("website", "https://mylabel.com"))
                .andExpect(model().attribute("releases", hasSize(1)))
                .andExpect(model().attribute("artists", hasSize(1)));
    }

    @Test
    void label_returns404_whenResourceNotFound() throws Exception {
        when(labelCRUDHandler.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createLabel_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels")
                        .with(user(testUser))
                        .with(csrf())
                        .param("labelName", "New Label")
                        .param("email", "info@newlabel.com")
                        .param("website", "https://newlabel.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(labelCRUDHandler).createLabel(
                "New Label", "info@newlabel.com", "https://newlabel.com", null, null, 1L);
    }

    @Test
    void updateLabel_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(put("/labels/1")
                        .with(user(testUser))
                        .with(csrf())
                        .param("labelName", "Updated Label")
                        .param("email", "updated@label.com")
                        .param("website", "https://updated.com")
                        .param("ownerName", "New Owner")
                        .param("street", "456 New St")
                        .param("street2", "Suite 100")
                        .param("city", "Bergen")
                        .param("postalCode", "5020")
                        .param("country", "Norway"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(labelCRUDHandler).updateLabel(
                1L,
                "Updated Label",
                "updated@label.com",
                "https://updated.com",
                new Address("456 New St", "Suite 100", "Bergen", "5020", "Norway"),
                new Person("New Owner")
        );
    }

}
