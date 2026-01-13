package org.omt.labelmanager.overview;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelService;
import org.omt.labelmanager.overview.api.OverviewController;
import org.omt.labelmanager.release.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OverviewController.class)
class OverviewControllerTest {

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
        var mockLabel = new Label(1L,"Mock Label");
        when(labelService.getAllLabels()).thenReturn(List.of(mockLabel));

        mockMvc.perform(get("/overview"))
                .andExpect(status().isOk())
                .andExpect(view().name("overview"))
                .andExpect(model().attribute("labels", List.of(mockLabel)));
    }

}