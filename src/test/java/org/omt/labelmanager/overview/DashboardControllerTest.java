package org.omt.labelmanager.overview;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.dashboard.api.DashboardController;
import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelService;
import org.omt.labelmanager.release.ReleaseCRUDHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelService labelService;

    @MockitoBean
    private ReleaseCRUDHandler releaseCRUDHandler;

    @Test
    void dashboard_greetsUser() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("user", "Alex The Manager"));
    }

    @Test
    void dashboard_showsListOfLabels() throws Exception {
        var mockLabel = new Label(1L,"Mock Label");
        when(labelService.getAllLabels()).thenReturn(List.of(mockLabel));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("labels", List.of(mockLabel)));
    }

}