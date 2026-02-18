package org.omt.labelmanager.catalog.release.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.ArtistFactory;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.ReleaseFactory;
import org.omt.labelmanager.catalog.release.TrackFactory;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.DistributorFactory;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocationFactory;
import org.omt.labelmanager.inventory.api.ProductionRunWithAllocation;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRunFactory;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReleaseController.class)
@Import(TestSecurityConfig.class)
class ReleaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabelQueryApi labelQueryFacade;

    @MockitoBean
    private ReleaseCommandApi releaseCommandFacade;

    @MockitoBean
    private ReleaseQueryApi releaseQueryFacade;

    @MockitoBean
    private ArtistQueryApi artistQueryApi;

    @MockitoBean
    private CostQueryApi costQueryFacade;

    @MockitoBean
    private ProductionRunQueryApi productionRunQueryService;

    @MockitoBean
    private AllocationQueryApi allocationQueryService;

    @MockitoBean
    private DistributorQueryApi distributorQueryService;

    @MockitoBean
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @MockitoBean
    private SaleQueryApi saleQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void release_returnsReleaseViewAndPopulatedModel() throws Exception {
        var label = LabelFactory.aLabel().id(1L).name("My Label").build();
        var releaseDate = LocalDate.now();
        var artist = ArtistFactory.anArtist().id(1L).artistName("Test Artist").build();
        var anotherArtist = ArtistFactory.anArtist().id(2L).artistName("Another Artist").build();
        var track = TrackFactory.aTrack()
                .artistId(1L)
                .name("Test Track")
                .durationSeconds(210)
                .position(1)
                .build();
        var formats = Set.of(ReleaseFormat.DIGITAL, ReleaseFormat.VINYL);
        var release = ReleaseFactory.aRelease()
                .id(4L)
                .name("First Release")
                .releaseDate(releaseDate)
                .labelId(label.id())
                .artistId(1L)
                .tracks(List.of(track))
                .formats(formats)
                .build();

        when(labelQueryFacade.findById(1L)).thenReturn(Optional.of(label));
        when(releaseQueryFacade.findById(4L)).thenReturn(Optional.of(release));
        when(artistQueryApi.getArtistsForUser(1L)).thenReturn(List.of(artist, anotherArtist));

        mockMvc.perform(get("/labels/1/releases/4").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("/releases/release"))
                .andExpect(model().attribute("name", "First Release"))
                .andExpect(model().attribute("labelId", 1L))
                .andExpect(model().attribute("releaseId", 4L))
                .andExpect(model().attribute("releaseDate", releaseDate))
                .andExpect(model().attribute("artists", List.of(artist)))
                .andExpect(model().attributeExists("tracks"))
                .andExpect(model().attribute("formats", formats))
                .andExpect(model().attribute("allArtists", List.of(artist, anotherArtist)))
                .andExpect(model().attribute("allFormats", ReleaseFormat.values()));
    }

    @Test
    void release_populatesInventoryDataInProductionRuns() throws Exception {
        var release = ReleaseFactory.aRelease().id(4L).labelId(1L).build();
        var productionRun = ProductionRunFactory.aProductionRun()
                .id(10L).releaseId(4L).quantity(500).build();

        when(releaseQueryFacade.findById(4L)).thenReturn(Optional.of(release));
        when(productionRunQueryService.findByReleaseId(4L)).thenReturn(List.of(productionRun));
        when(inventoryMovementQueryApi.getWarehouseInventory(10L)).thenReturn(200);
        when(inventoryMovementQueryApi.getCurrentInventoryByDistributor(10L))
                .thenReturn(Map.of());
        when(inventoryMovementQueryApi.getMovementsForProductionRun(10L))
                .thenReturn(List.of());

        var result = mockMvc.perform(get("/labels/1/releases/4").with(user(testUser)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        var productionRuns = (List<ProductionRunWithAllocation>) result.getModelAndView()
                .getModel().get("productionRuns");
        assertThat(productionRuns).hasSize(1);
        assertThat(productionRuns.get(0).warehouseInventory()).isEqualTo(200);
        assertThat(productionRuns.get(0).distributorInventories()).isEmpty();
        assertThat(productionRuns.get(0).movements()).isEmpty();
    }

    @Test
    void release_populatesNonEmptyDistributorInventories() throws Exception {
        var release = ReleaseFactory.aRelease().id(4L).labelId(1L).build();
        var productionRun = ProductionRunFactory.aProductionRun()
                .id(10L).releaseId(4L).quantity(500).build();
        var alphaDistributor = DistributorFactory.aDistributor()
                .id(1L).name("Alpha Records").build();
        var betaDistributor = DistributorFactory.aDistributor()
                .id(2L).name("Beta Distribution").build();
        // Distributor 1 has an allocation; distributor 2 does not (union edge case)
        var allocation = ChannelAllocationFactory.aChannelAllocation()
                .productionRunId(10L).distributorId(1L).quantity(100).build();

        when(releaseQueryFacade.findById(4L)).thenReturn(Optional.of(release));
        when(productionRunQueryService.findByReleaseId(4L)).thenReturn(List.of(productionRun));
        when(distributorQueryService.findByLabelId(1L))
                .thenReturn(List.of(alphaDistributor, betaDistributor));
        when(allocationQueryService.getAllocationsForProductionRun(10L))
                .thenReturn(List.of(allocation));
        when(inventoryMovementQueryApi.getWarehouseInventory(10L)).thenReturn(350);
        when(inventoryMovementQueryApi.getCurrentInventoryByDistributor(10L))
                .thenReturn(Map.of(1L, 80, 2L, 30));
        when(inventoryMovementQueryApi.getMovementsForProductionRun(10L))
                .thenReturn(List.of());

        var result = mockMvc.perform(get("/labels/1/releases/4").with(user(testUser)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        var productionRuns = (List<ProductionRunWithAllocation>) result.getModelAndView()
                .getModel().get("productionRuns");
        var distributorInventories = productionRuns.get(0).distributorInventories();
        assertThat(distributorInventories).hasSize(2);

        // Results are sorted alphabetically by name
        var alpha = distributorInventories.get(0);
        assertThat(alpha.name()).isEqualTo("Alpha Records");
        assertThat(alpha.allocated()).isEqualTo(100);
        assertThat(alpha.current()).isEqualTo(80);
        assertThat(alpha.sold()).isEqualTo(20);

        // Beta appears only in currentByDistributor, not in allocations (union edge case)
        var beta = distributorInventories.get(1);
        assertThat(beta.name()).isEqualTo("Beta Distribution");
        assertThat(beta.allocated()).isEqualTo(0);
        assertThat(beta.current()).isEqualTo(30);
        assertThat(beta.sold()).isEqualTo(-30);
    }

    @Test
    void release_returns404_whenResourceNotFound() throws Exception {
        when(labelQueryFacade.findById(1123L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/labels/1123").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRelease_callsHandlerAndRedirectsToLabel() throws Exception {
        mockMvc.perform(delete("/labels/1/releases/5")
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(releaseCommandFacade).delete(5L);
    }

    @Test
    void updateRelease_callsHandlerAndRedirectsToRelease() throws Exception {
        mockMvc.perform(put("/labels/1/releases/5")
                        .with(user(testUser))
                        .with(csrf())
                        .param("releaseName", "Updated Release")
                        .param("releaseDate", "2026-06-15")
                        .param("artistIds", "1", "2")
                        .param("tracks[0].name", "Track 1")
                        .param("tracks[0].duration", "3:30")
                        .param("tracks[0].artistIds", "1")
                        .param("formats", "VINYL", "CD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/5"));

        verify(releaseCommandFacade).updateRelease(
                org.mockito.ArgumentMatchers.eq(5L),
                org.mockito.ArgumentMatchers.eq("Updated Release"),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 6, 15)),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anySet()
        );
    }

    @Test
    void release_populatesReleaseSalesAcrossProductionRuns() throws Exception {
        var release = ReleaseFactory.aRelease().id(4L).labelId(1L).build();
        var productionRun = ProductionRunFactory.aProductionRun()
                .id(10L).releaseId(4L).quantity(500).build();
        var distributor = DistributorFactory.aDistributor()
                .id(1L).name("Cargo Records").channelType(ChannelType.DISTRIBUTOR).build();
        var lineItem = new SaleLineItem(1L, 4L,
                org.omt.labelmanager.catalog.release.domain.ReleaseFormat.VINYL,
                30, Money.of(BigDecimal.valueOf(15)), Money.of(BigDecimal.valueOf(450)));
        var sale = new Sale(10L, 1L, 1L, LocalDate.of(2026, 1, 10),
                ChannelType.DISTRIBUTOR, null, List.of(lineItem),
                Money.of(BigDecimal.valueOf(450)));

        when(releaseQueryFacade.findById(4L)).thenReturn(Optional.of(release));
        when(productionRunQueryService.findByReleaseId(4L)).thenReturn(List.of(productionRun));
        when(distributorQueryService.findByLabelId(1L)).thenReturn(List.of(distributor));
        when(inventoryMovementQueryApi.getCurrentInventoryByDistributor(10L))
                .thenReturn(Map.of());
        when(inventoryMovementQueryApi.getMovementsForProductionRun(10L)).thenReturn(List.of());
        when(saleQueryApi.getSalesForProductionRun(10L)).thenReturn(List.of(sale));

        var result = mockMvc.perform(get("/labels/1/releases/4").with(user(testUser)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        var releaseSales = (List<ReleaseSaleView>) result.getModelAndView()
                .getModel().get("releaseSales");
        assertThat(releaseSales).hasSize(1);
        assertThat(releaseSales.get(0).distributorName()).isEqualTo("Cargo Records");
        assertThat(releaseSales.get(0).totalUnits()).isEqualTo(30);
        assertThat(releaseSales.get(0).totalRevenue().amount())
                .isEqualByComparingTo(BigDecimal.valueOf(450));

        assertThat(result.getModelAndView().getModel().get("totalUnitsSold")).isEqualTo(30);
    }
}
