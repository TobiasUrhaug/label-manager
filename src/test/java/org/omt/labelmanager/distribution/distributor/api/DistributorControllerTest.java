package org.omt.labelmanager.distribution.distributor.api;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DistributorController.class)
@Import(TestSecurityConfig.class)
class DistributorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DistributorCommandApi distributorCRUDHandler;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void addDistributor_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels/1/distributors")
                        .with(user(testUser))
                        .with(csrf())
                        .param("name", "Bandcamp")
                        .param("channelType", "DIRECT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(distributorCRUDHandler).createDistributor(eq(1L), eq("Bandcamp"), eq(ChannelType.DIRECT));
    }

    @Test
    void addDistributor_worksWithDistributorType() throws Exception {
        mockMvc
                .perform(post("/labels/1/distributors")
                        .with(user(testUser))
                        .with(csrf())
                        .param("name", "Cargo Records")
                        .param("channelType", "DISTRIBUTOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(distributorCRUDHandler).createDistributor(
                eq(1L),
                eq("Cargo Records"),
                eq(ChannelType.DISTRIBUTOR)
        );
    }

    @Test
    void addDistributor_worksWithRetailType() throws Exception {
        mockMvc
                .perform(post("/labels/1/distributors")
                        .with(user(testUser))
                        .with(csrf())
                        .param("name", "Local Record Shop")
                        .param("channelType", "RETAIL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(distributorCRUDHandler).createDistributor(
                eq(1L),
                eq("Local Record Shop"),
                eq(ChannelType.RETAIL)
        );
    }

    @Test
    void deleteDistributor_callsHandlerAndRedirects() throws Exception {
        when(distributorCRUDHandler.delete(99L)).thenReturn(true);

        mockMvc
                .perform(delete("/labels/1/distributors/99")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(distributorCRUDHandler).delete(99L);
    }
}
