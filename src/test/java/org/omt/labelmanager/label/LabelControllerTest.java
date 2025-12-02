package org.omt.labelmanager.label;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabelController.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelService labelService;

    @Test
    void labels_showsUser() throws Exception {
        mockMvc.perform(get("/labels"))
                .andExpect(status().isOk())
                .andExpect(view().name("labels"))
                .andExpect(model().attribute("user", "Alex The Manager"));
    }

    @Test
    void labels_showsListOfLabels() throws Exception {
        var mockLabel = new Label("Mock Label");
        when(labelService.getAllLabels()).thenReturn(List.of(mockLabel));

        mockMvc.perform(get("/labels"))
                .andExpect(status().isOk())
                .andExpect(view().name("labels"))
                .andExpect(model().attribute("labels", List.of(mockLabel)));
    }
}