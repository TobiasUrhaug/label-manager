package org.omt.labelmanager.label;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.label.api.LabelController;
import org.omt.labelmanager.release.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabelController.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelService labelService;

    @MockitoBean
    private ReleaseService releaseService;

    @Test
    void label_redirectsToALabel() throws Exception {
        var mockLabel = new Label(1L, "My Label");
        when(labelService.findById(1L)).thenReturn(Optional.of(mockLabel));

        mockMvc.perform(get("/labels/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("labels/label"))
                .andExpect(model().attribute("name", "My Label"));
    }

    @Test
    void label_returns404_whenResourceNotFound() throws Exception {
        when(labelService.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123"))
                .andExpect(status().isNotFound());
    }

}