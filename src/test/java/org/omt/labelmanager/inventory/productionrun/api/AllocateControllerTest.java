package org.omt.labelmanager.inventory.productionrun.api;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.domain.InventoryLocation;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AllocateController.class)
@Import(TestSecurityConfig.class)
class AllocateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductionRunCommandApi productionRunCommandApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void allocate_toDistributor_redirectsAndDelegates() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("locationType", "DISTRIBUTOR")
                        .param("distributorId", "5")
                        .param("quantity", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"));

        verify(productionRunCommandApi).allocate(3L, InventoryLocation.distributor(5L), 100);
    }

    @Test
    void allocate_toBandcamp_redirectsAndDelegates() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("locationType", "BANDCAMP")
                        .param("quantity", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"));

        verify(productionRunCommandApi).allocate(3L, InventoryLocation.bandcamp(), 50);
    }

    @Test
    void allocate_overLimit_flashesAllocationError() throws Exception {
        doThrow(new InsufficientInventoryException(200, 50))
                .when(productionRunCommandApi).allocate(3L, InventoryLocation.distributor(5L), 200);

        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("locationType", "DISTRIBUTOR")
                        .param("distributorId", "5")
                        .param("quantity", "200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"))
                .andExpect(flash().attributeExists("allocationError"));
    }

    @Test
    void allocate_withZeroQuantity_flashesAllocationError() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("locationType", "DISTRIBUTOR")
                        .param("distributorId", "5")
                        .param("quantity", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"))
                .andExpect(flash().attributeExists("allocationError"));
    }

    @Test
    void allocate_withNullLocationType_flashesAllocationError() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("quantity", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"))
                .andExpect(flash().attributeExists("allocationError"));
    }

    @Test
    void allocate_distributorWithoutDistributorId_flashesAllocationError() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("locationType", "DISTRIBUTOR")
                        .param("quantity", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"))
                .andExpect(flash().attributeExists("allocationError"));
    }

    @Test
    void cancelBandcampReservation_redirectsAndDelegates() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/bandcamp-cancellations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("quantity", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"));

        verify(productionRunCommandApi).cancelBandcampReservation(3L, 30);
    }

    @Test
    void cancelBandcampReservation_overHeldQuantity_flashesCancellationError() throws Exception {
        doThrow(new InsufficientInventoryException(50, 20))
                .when(productionRunCommandApi).cancelBandcampReservation(3L, 50);

        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/bandcamp-cancellations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("quantity", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"))
                .andExpect(flash().attributeExists("cancellationError"));
    }

    @Test
    void cancelBandcampReservation_withZeroQuantity_flashesCancellationError() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/bandcamp-cancellations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("quantity", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"))
                .andExpect(flash().attributeExists("cancellationError"));
    }
}
