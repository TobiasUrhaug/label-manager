package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.allocation.application.CreateAllocationUseCase;
import org.omt.labelmanager.inventory.allocation.domain.InsufficientInventoryException;
import org.omt.labelmanager.inventory.allocation.api.AllocationController;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AllocationController.class)
@Import(TestSecurityConfig.class)
class AllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateAllocationUseCase allocateProductionRunToDistributorUseCase;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void addAllocation_callsHandlerAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("distributorId", "5")
                        .param("quantity", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"));

        verify(allocateProductionRunToDistributorUseCase).execute(eq(3L), eq(5L), eq(100));
    }

    @Test
    void addAllocation_addsFlashErrorOnInsufficientInventory() throws Exception {
        doThrow(new InsufficientInventoryException(200, 100))
                .when(allocateProductionRunToDistributorUseCase).execute(anyLong(), anyLong(), anyInt());

        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("distributorId", "5")
                        .param("quantity", "200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/2"))
                .andExpect(flash().attributeExists("allocationError"));
    }

    @Test
    void addAllocation_flashErrorContainsExceptionMessage() throws Exception {
        doThrow(new InsufficientInventoryException(200, 50))
                .when(allocateProductionRunToDistributorUseCase).execute(anyLong(), anyLong(), anyInt());

        mockMvc
                .perform(post("/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .param("distributorId", "5")
                        .param("quantity", "200"))
                .andExpect(flash().attribute(
                        "allocationError",
                        "Insufficient inventory: requested 200 but only 50 available"
                ));
    }
}
