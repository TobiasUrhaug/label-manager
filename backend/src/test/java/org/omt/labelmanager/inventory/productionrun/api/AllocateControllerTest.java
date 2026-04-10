package org.omt.labelmanager.inventory.productionrun.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void allocate_toDistributor_returnsNoContent() throws Exception {
        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"locationType": "DISTRIBUTOR", "distributorId": 5, "quantity": 100}
                                """))
                .andExpect(status().isNoContent());

        verify(productionRunCommandApi).allocate(3L, InventoryLocation.distributor(5L), 100);
    }

    @Test
    void allocate_toBandcamp_returnsNoContent() throws Exception {
        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"locationType": "BANDCAMP", "quantity": 50}
                                """))
                .andExpect(status().isNoContent());

        verify(productionRunCommandApi).allocate(3L, InventoryLocation.bandcamp(), 50);
    }

    @Test
    void allocate_overLimit_returnsBadRequest() throws Exception {
        doThrow(new InsufficientInventoryException(200, 50))
                .when(productionRunCommandApi).allocate(3L, InventoryLocation.distributor(5L), 200);

        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"locationType": "DISTRIBUTOR", "distributorId": 5, "quantity": 200}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allocate_withZeroQuantity_returnsBadRequest() throws Exception {
        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"locationType": "DISTRIBUTOR", "distributorId": 5, "quantity": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allocate_withNullLocationType_returnsBadRequest() throws Exception {
        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 50}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allocate_distributorWithoutDistributorId_returnsBadRequest() throws Exception {
        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/allocations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"locationType": "DISTRIBUTOR", "quantity": 50}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelBandcampReservation_returnsNoContent() throws Exception {
        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/bandcamp-cancellations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 30}
                                """))
                .andExpect(status().isNoContent());

        verify(productionRunCommandApi).cancelBandcampReservation(3L, 30);
    }

    @Test
    void cancelBandcampReservation_overHeldQuantity_returnsBadRequest() throws Exception {
        doThrow(new InsufficientInventoryException(50, 20))
                .when(productionRunCommandApi).cancelBandcampReservation(3L, 50);

        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/bandcamp-cancellations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 50}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelBandcampReservation_withZeroQuantity_returnsBadRequest() throws Exception {
        mockMvc
                .perform(post("/api/labels/1/releases/2/production-runs/3/bandcamp-cancellations")
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"quantity": 0}
                                """))
                .andExpect(status().isBadRequest());
    }
}
