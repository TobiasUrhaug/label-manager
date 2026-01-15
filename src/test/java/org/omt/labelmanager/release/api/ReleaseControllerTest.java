package org.omt.labelmanager.release.api;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.label.LabelCRUDHandler;
import org.omt.labelmanager.label.LabelFactory;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.omt.labelmanager.release.ReleaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        var label = LabelFactory.builder().id(1L).name("My Label").build();
        var releaseDate = LocalDate.now();
        var release = ReleaseFactory
                .builder()
                .id(4L)
                .name("First Release")
                .releaseDate(releaseDate)
                .label(label)
                .build();

        when(labelCRUDHandler.findById(1L)).thenReturn(Optional.of(label));
        when(releaseCRUDHandler.findById(4L)).thenReturn(Optional.of(release));

        mockMvc.perform(get("/labels/1/releases/4"))
                .andExpect(status().isOk())
                .andExpect(view().name("/releases/release"))
                .andExpect(model().attribute("name", "First Release"))
                .andExpect(model().attribute("labelId", 1L))
                .andExpect(model().attribute("releaseId", 4L))
                .andExpect(model().attribute("releaseDate", releaseDate));
    }

    @Test
    void release_returns404_whenResourceNotFound() throws Exception {
        when(labelCRUDHandler.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123"))
                .andExpect(status().isNotFound());
    }

}