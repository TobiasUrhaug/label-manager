package org.omt.labelmanager.release;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelService;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.release.api.ReleaseController;
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
class ReleaseEntityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelService labelService;

    @MockitoBean
    private ReleaseCRUDHandler releaseCRUDHandler;

    @Test
    void release_returnsReleaseViewAndPopulatedModel() throws Exception {
        LabelEntity mockLabelEntity = new LabelEntity( "My Label");
        Label mockLabel = new Label(1L, "My Label");
        LocalDate releaseDate = LocalDate.now();
        Release mockRelease = new Release(4L, "First Release", releaseDate, mockLabel);
        when(labelService.findById(1L)).thenReturn(Optional.of(mockLabel));
        when(releaseCRUDHandler.findById(4L)).thenReturn(Optional.of(mockRelease));

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
        when(labelService.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123"))
                .andExpect(status().isNotFound());
    }

}