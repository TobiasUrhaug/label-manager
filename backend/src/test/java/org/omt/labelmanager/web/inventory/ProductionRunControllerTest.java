package org.omt.labelmanager.web.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.distribution.distributor.DistributorFactory;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRunFactory;
import org.omt.labelmanager.shared.Format;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductionRunController.class)
@Import(TestSecurityConfig.class)
class ProductionRunControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ProductionRunCommandApi commandApi;

    @MockitoBean private ProductionRunQueryApi queryApi;

    @MockitoBean private InventoryMovementQueryApi inventoryMovementQueryApi;

    @MockitoBean private DistributorQueryApi distributorQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void addProductionRun_callsHandlerAndReturnsCreated() throws Exception {
        mockMvc.perform(
                        post("/api/labels/1/releases/42/production-runs")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "format": "VINYL",
                                  "description": "Original pressing",
                                  "manufacturer": "Record Industry",
                                  "manufacturingDate": "2025-01-01",
                                  "quantity": 500
                                }
                                """))
                .andExpect(status().isCreated());

        verify(commandApi)
                .createProductionRun(
                        eq(42L),
                        eq(Format.VINYL),
                        eq("Original pressing"),
                        eq("Record Industry"),
                        eq(LocalDate.of(2025, 1, 1)),
                        eq(500));
    }

    @Test
    void addProductionRun_worksWithCDFormat() throws Exception {
        mockMvc.perform(
                        post("/api/labels/1/releases/42/production-runs")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "format": "CD",
                                  "description": "Initial run",
                                  "manufacturer": "CD Plant",
                                  "manufacturingDate": "2025-01-15",
                                  "quantity": 200
                                }
                                """))
                .andExpect(status().isCreated());

        verify(commandApi)
                .createProductionRun(
                        eq(42L),
                        eq(Format.CD),
                        eq("Initial run"),
                        eq("CD Plant"),
                        eq(LocalDate.of(2025, 1, 15)),
                        eq(200));
    }

    @Test
    void addProductionRun_worksWithCassetteFormat() throws Exception {
        mockMvc.perform(
                        post("/api/labels/1/releases/42/production-runs")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "format": "CASSETTE",
                                  "description": "Limited edition",
                                  "manufacturer": "Tape Factory",
                                  "manufacturingDate": "2025-02-01",
                                  "quantity": 100
                                }
                                """))
                .andExpect(status().isCreated());

        verify(commandApi)
                .createProductionRun(eq(42L), eq(Format.CASSETTE), any(), any(), any(), eq(100));
    }

    @Test
    void deleteProductionRun_callsHandlerAndReturnsNoContent() throws Exception {
        when(commandApi.delete(99L)).thenReturn(true);

        mockMvc.perform(
                        delete("/api/labels/1/releases/42/production-runs/99")
                                .with(user(testUser))
                                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(commandApi).delete(99L);
    }

    @Test
    void productionRuns_populatesInventoryData() throws Exception {
        var productionRun =
                ProductionRunFactory.aProductionRun().id(10L).releaseId(4L).quantity(500).build();

        when(queryApi.findByReleaseId(4L)).thenReturn(List.of(productionRun));
        when(inventoryMovementQueryApi.getWarehouseInventory(10L)).thenReturn(200);
        when(inventoryMovementQueryApi.getBandcampInventory(10L)).thenReturn(25);
        when(inventoryMovementQueryApi.getCurrentInventoryByDistributor(10L)).thenReturn(Map.of());
        when(inventoryMovementQueryApi.getMovementsForProductionRun(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/labels/1/releases/4/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseInventory").value(700))
                .andExpect(jsonPath("$[0].bandcampInventory").value(25))
                .andExpect(jsonPath("$[0].distributorInventories").isEmpty())
                .andExpect(jsonPath("$[0].movements").isEmpty());
    }

    @Test
    void productionRuns_namesDistributorsInInventories() throws Exception {
        var productionRun =
                ProductionRunFactory.aProductionRun().id(10L).releaseId(4L).quantity(500).build();
        var alpha = DistributorFactory.aDistributor().id(1L).name("Alpha Records").build();
        var beta = DistributorFactory.aDistributor().id(2L).name("Beta Distribution").build();

        when(queryApi.findByReleaseId(4L)).thenReturn(List.of(productionRun));
        when(distributorQueryApi.findByLabelId(1L)).thenReturn(List.of(alpha, beta));
        when(inventoryMovementQueryApi.getWarehouseInventory(10L)).thenReturn(350);
        when(inventoryMovementQueryApi.getCurrentInventoryByDistributor(10L))
                .thenReturn(Map.of(1L, 80, 2L, 30));
        when(inventoryMovementQueryApi.getMovementsForProductionRun(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/labels/1/releases/4/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distributorInventories[0].name").value("Alpha Records"))
                .andExpect(jsonPath("$[0].distributorInventories[0].current").value(80))
                .andExpect(
                        jsonPath("$[0].distributorInventories[1].name").value("Beta Distribution"))
                .andExpect(jsonPath("$[0].distributorInventories[1].current").value(30));
    }
}
