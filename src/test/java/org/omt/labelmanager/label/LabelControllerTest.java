package org.omt.labelmanager.label;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.release.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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
    void overview_showsUser() throws Exception {
        mockMvc.perform(get("/overview"))
                .andExpect(status().isOk())
                .andExpect(view().name("overview"))
                .andExpect(model().attribute("user", "Alex The Manager"));
    }

    @Test
    void overview_showsListOfLabels() throws Exception {
        var mockLabel = new Label("Mock Label");
        when(labelService.getAllLabels()).thenReturn(List.of(mockLabel));

        mockMvc.perform(get("/overview"))
                .andExpect(status().isOk())
                .andExpect(view().name("overview"))
                .andExpect(model().attribute("labels", List.of(mockLabel)));
    }

    @Test
    void label_redirectsToALabel() throws Exception {
        var mockLabel = new Label( "My Label");
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