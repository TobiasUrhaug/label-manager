package org.omt.labelmanager.label.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.label.LabelFactory;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.omt.labelmanager.release.ReleaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LabelController.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelCRUDHandler labelCRUDHandler;

    @MockitoBean
    private ReleaseCRUDHandler releaseCRUDHandler;

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

        mockMvc
                .perform(get("/labels/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("labels/label"))
                .andExpect(model().attribute("name", "My Label"))
                .andExpect(model().attribute("id", 1L))
                .andExpect(model().attribute("email", "contact@mylabel.com"))
                .andExpect(model().attribute("website", "https://mylabel.com"))
                .andExpect(model().attribute("releases", hasSize(1)));
    }

    @Test
    void label_returns404_whenResourceNotFound() throws Exception {
        when(labelCRUDHandler.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createLabel_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels")
                        .param("labelName", "New Label")
                        .param("email", "info@newlabel.com")
                        .param("website", "https://newlabel.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(labelCRUDHandler).createLabel("New Label", "info@newlabel.com", "https://newlabel.com");
    }

}