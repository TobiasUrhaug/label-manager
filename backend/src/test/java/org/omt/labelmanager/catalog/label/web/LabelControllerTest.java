package org.omt.labelmanager.catalog.label.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LabelController.class)
@Import(TestSecurityConfig.class)
class LabelControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LabelCommandApi labelCommandFacade;

    @MockitoBean private LabelQueryApi labelQueryFacade;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void label_returnsLabelJson() throws Exception {
        var label =
                LabelFactory.aLabel()
                        .id(1L)
                        .name("My Label")
                        .email("contact@mylabel.com")
                        .website("https://mylabel.com")
                        .build();
        when(labelQueryFacade.findById(1L)).thenReturn(Optional.of(label));

        mockMvc.perform(get("/api/labels/1").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Label"))
                .andExpect(jsonPath("$.email").value("contact@mylabel.com"))
                .andExpect(jsonPath("$.website").value("https://mylabel.com"));
    }

    @Test
    void label_doesNotBundleReleasesArtistsOrDistributors() throws Exception {
        var label = LabelFactory.aLabel().id(1L).name("My Label").build();
        when(labelQueryFacade.findById(1L)).thenReturn(Optional.of(label));

        mockMvc.perform(get("/api/labels/1").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releases").doesNotExist())
                .andExpect(jsonPath("$.artists").doesNotExist())
                .andExpect(jsonPath("$.distributors").doesNotExist());
    }

    @Test
    void label_returns404ProblemDetail_whenResourceNotFound() throws Exception {
        when(labelQueryFacade.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/labels/1123").with(user(testUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Label not found: 1123"));
    }

    @Test
    void createLabel_returnsCreated() throws Exception {
        mockMvc.perform(
                        post("/api/labels")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "labelName": "New Label",
                                  "email": "info@newlabel.com",
                                  "website": "https://newlabel.com"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(labelCommandFacade)
                .createLabel(
                        "New Label", "info@newlabel.com", "https://newlabel.com", null, null, 1L);
    }

    @Test
    void updateLabel_returnsNoContent() throws Exception {
        mockMvc.perform(
                        put("/api/labels/1")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
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

        verify(labelCommandFacade)
                .updateLabel(
                        1L,
                        "Updated Label",
                        "updated@label.com",
                        "https://updated.com",
                        new Address("456 New St", "Suite 100", "Bergen", "5020", "Norway"),
                        new Person("New Owner"));
    }

    @Test
    void deleteLabel_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/labels/1").with(user(testUser)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(labelCommandFacade).delete(1L);
    }
}
