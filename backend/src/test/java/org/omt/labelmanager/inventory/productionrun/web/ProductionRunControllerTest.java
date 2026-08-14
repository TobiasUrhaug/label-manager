package org.omt.labelmanager.inventory.productionrun.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.api.LocationBalance;
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

    @MockitoBean private ReleaseQueryApi releaseQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @BeforeEach
    void scopeChecksPass() {
        when(releaseQueryApi.belongsToLabel(anyLong(), anyLong())).thenReturn(true);
    }

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
        when(queryApi.findById(99L))
                .thenReturn(
                        Optional.of(
                                ProductionRunFactory.aProductionRun()
                                        .id(99L)
                                        .releaseId(42L)
                                        .build()));
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
        // Reported as the ledger gives it. The run's 500 manufactured units are a PRODUCTION
        // movement (V33), so the controller no longer adds them back in.
        when(inventoryMovementQueryApi.balancesFor(List.of(10L)))
                .thenReturn(
                        List.of(
                                new LocationBalance(10L, InventoryLocation.warehouse(), 200),
                                new LocationBalance(10L, InventoryLocation.bandcamp(), 25)));
        when(inventoryMovementQueryApi.findByProductionRunIds(List.of(10L))).thenReturn(Map.of());

        mockMvc.perform(get("/api/labels/1/releases/4/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseInventory").value(200))
                .andExpect(jsonPath("$[0].bandcampInventory").value(25))
                .andExpect(jsonPath("$[0].distributorInventories").isEmpty())
                .andExpect(jsonPath("$[0].movements").isEmpty());
    }

    @Test
    void productionRuns_reportsDistributorInventoriesById() throws Exception {
        var productionRun =
                ProductionRunFactory.aProductionRun().id(10L).releaseId(4L).quantity(500).build();
        when(queryApi.findByReleaseId(4L)).thenReturn(List.of(productionRun));
        when(inventoryMovementQueryApi.balancesFor(List.of(10L)))
                .thenReturn(
                        List.of(
                                new LocationBalance(10L, InventoryLocation.warehouse(), 350),
                                new LocationBalance(10L, InventoryLocation.distributor(2L), 30),
                                new LocationBalance(10L, InventoryLocation.distributor(1L), 80)));
        when(inventoryMovementQueryApi.findByProductionRunIds(List.of(10L))).thenReturn(Map.of());

        mockMvc.perform(get("/api/labels/1/releases/4/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distributorInventories[0].distributorId").value(1))
                .andExpect(jsonPath("$[0].distributorInventories[0].current").value(80))
                .andExpect(jsonPath("$[0].distributorInventories[1].distributorId").value(2))
                .andExpect(jsonPath("$[0].distributorInventories[1].current").value(30));
    }

    /**
     * A negative distributor balance means a sale or return was recorded against stock that
     * distributor never held. It is a data error, and rendering it as inventory would present it as
     * a fact about stock.
     */
    @Test
    void productionRuns_omitsDistributorsWithANegativeBalance() throws Exception {
        var productionRun =
                ProductionRunFactory.aProductionRun().id(10L).releaseId(4L).quantity(500).build();

        when(queryApi.findByReleaseId(4L)).thenReturn(List.of(productionRun));
        when(inventoryMovementQueryApi.balancesFor(List.of(10L)))
                .thenReturn(
                        List.of(
                                new LocationBalance(10L, InventoryLocation.distributor(1L), -40),
                                new LocationBalance(10L, InventoryLocation.distributor(2L), 30)));
        when(inventoryMovementQueryApi.findByProductionRunIds(List.of(10L))).thenReturn(Map.of());

        mockMvc.perform(get("/api/labels/1/releases/4/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distributorInventories.length()").value(1))
                .andExpect(jsonPath("$[0].distributorInventories[0].distributorId").value(2));
    }

    /**
     * The page cost must not grow with the number of pressings — that is the N+1 this read model
     * used to have, four queries per run.
     */
    @Test
    void productionRuns_readsEveryPressingInOneRound() throws Exception {
        var first = ProductionRunFactory.aProductionRun().id(10L).releaseId(4L).build();
        var second = ProductionRunFactory.aProductionRun().id(11L).releaseId(4L).build();
        var third = ProductionRunFactory.aProductionRun().id(12L).releaseId(4L).build();

        when(queryApi.findByReleaseId(4L)).thenReturn(List.of(first, second, third));
        when(inventoryMovementQueryApi.balancesFor(List.of(10L, 11L, 12L)))
                .thenReturn(List.of(new LocationBalance(11L, InventoryLocation.warehouse(), 42)));
        when(inventoryMovementQueryApi.findByProductionRunIds(List.of(10L, 11L, 12L)))
                .thenReturn(Map.of());

        mockMvc.perform(get("/api/labels/1/releases/4/production-runs").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouseInventory").value(0))
                .andExpect(jsonPath("$[1].warehouseInventory").value(42))
                .andExpect(jsonPath("$[2].warehouseInventory").value(0));

        verify(inventoryMovementQueryApi).balancesFor(List.of(10L, 11L, 12L));
        verify(inventoryMovementQueryApi).findByProductionRunIds(List.of(10L, 11L, 12L));
        verifyNoMoreInteractions(inventoryMovementQueryApi);
    }

    @Test
    void productionRuns_returns404WhenReleaseBelongsToAnotherLabel() throws Exception {
        when(releaseQueryApi.belongsToLabel(4L, 1L)).thenReturn(false);

        mockMvc.perform(get("/api/labels/1/releases/4/production-runs").with(user(testUser)))
                .andExpect(status().isNotFound());

        verify(queryApi, org.mockito.Mockito.never()).findByReleaseId(any());
    }

    @Test
    void deleteProductionRun_returns404WhenTheRunBelongsToAnotherRelease() throws Exception {
        when(queryApi.findById(99L))
                .thenReturn(
                        Optional.of(
                                ProductionRunFactory.aProductionRun()
                                        .id(99L)
                                        .releaseId(7L)
                                        .build()));

        mockMvc.perform(
                        delete("/api/labels/1/releases/42/production-runs/99")
                                .with(user(testUser))
                                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(commandApi, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void addProductionRun_returns404WhenReleaseBelongsToAnotherLabel() throws Exception {
        when(releaseQueryApi.belongsToLabel(42L, 1L)).thenReturn(false);

        mockMvc.perform(
                        post("/api/labels/1/releases/42/production-runs")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content("{\"format\": \"VINYL\", \"quantity\": 10}"))
                .andExpect(status().isNotFound());

        verify(commandApi, org.mockito.Mockito.never())
                .createProductionRun(
                        any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }
}
