package org.omt.labelmanager.inventory.api;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.application.ProductionRunCRUDHandler;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductionRunController.class)
@Import(TestSecurityConfig.class)
class ProductionRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductionRunCRUDHandler productionRunCRUDHandler;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void addProductionRun_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/42/production-runs")
                        .with(user(testUser))
                        .with(csrf())
                        .param("format", "VINYL")
                        .param("description", "Original pressing")
                        .param("manufacturer", "Record Industry")
                        .param("manufacturingDate", "2025-01-01")
                        .param("quantity", "500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/42"));

        verify(productionRunCRUDHandler).create(
                eq(42L),
                eq(ReleaseFormat.VINYL),
                eq("Original pressing"),
                eq("Record Industry"),
                eq(LocalDate.of(2025, 1, 1)),
                eq(500)
        );
    }

    @Test
    void addProductionRun_worksWithCDFormat() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/42/production-runs")
                        .with(user(testUser))
                        .with(csrf())
                        .param("format", "CD")
                        .param("description", "Initial run")
                        .param("manufacturer", "CD Plant")
                        .param("manufacturingDate", "2025-01-15")
                        .param("quantity", "200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/42"));

        verify(productionRunCRUDHandler).create(
                eq(42L),
                eq(ReleaseFormat.CD),
                eq("Initial run"),
                eq("CD Plant"),
                eq(LocalDate.of(2025, 1, 15)),
                eq(200)
        );
    }

    @Test
    void addProductionRun_worksWithCassetteFormat() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/42/production-runs")
                        .with(user(testUser))
                        .with(csrf())
                        .param("format", "CASSETTE")
                        .param("description", "Limited edition")
                        .param("manufacturer", "Tape Factory")
                        .param("manufacturingDate", "2025-02-01")
                        .param("quantity", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/42"));

        verify(productionRunCRUDHandler).create(
                eq(42L),
                eq(ReleaseFormat.CASSETTE),
                any(),
                any(),
                any(),
                eq(100)
        );
    }

    @Test
    void deleteProductionRun_callsHandlerAndRedirects() throws Exception {
        when(productionRunCRUDHandler.delete(99L)).thenReturn(true);

        mockMvc
                .perform(delete("/labels/1/releases/42/production-runs/99")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/42"));

        verify(productionRunCRUDHandler).delete(99L);
    }
}
