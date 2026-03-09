package org.omt.labelmanager.distribution.distributor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.DistributorFactory;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DistributorController.class)
@Import(TestSecurityConfig.class)
class DistributorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DistributorCommandApi distributorCRUDHandler;

    @MockitoBean
    private DistributorQueryApi distributorQueryApi;

    @MockitoBean
    private LabelQueryApi labelQueryApi;

    @MockitoBean
    private SaleQueryApi saleQueryApi;

    @MockitoBean
    private DistributorReturnQueryApi returnQueryApi;

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

        verify(distributorCRUDHandler)
                .createDistributor(eq(1L), eq("Bandcamp"), eq(ChannelType.DIRECT));
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
    void addDistributor_worksWithRecordStoreType() throws Exception {
        mockMvc
                .perform(post("/labels/1/distributors")
                        .with(user(testUser))
                        .with(csrf())
                        .param("name", "Local Record Shop")
                        .param("channelType", "RECORD_STORE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1"));

        verify(distributorCRUDHandler).createDistributor(
                eq(1L),
                eq("Local Record Shop"),
                eq(ChannelType.RECORD_STORE)
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

    @Test
    void showDistributor_returnsDetailViewWithSalesAndReturns() throws Exception {
        var label = LabelFactory.aLabel().id(1L).name("My Label").build();
        var distributor = DistributorFactory.aDistributor()
                .id(5L).labelId(1L).name("Cargo Records").channelType(ChannelType.DISTRIBUTOR)
                .build();
        var sale = new Sale(10L, 1L, 5L, LocalDate.of(2026, 1, 10),
                ChannelType.DISTRIBUTOR, null, List.of(), null);
        var distributorReturn = new DistributorReturn(
                20L, 1L, 5L, LocalDate.of(2026, 2, 1), null, List.of(), null);

        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(5L)).thenReturn(Optional.of(distributor));
        when(saleQueryApi.getSalesForDistributor(5L)).thenReturn(List.of(sale));
        when(returnQueryApi.getReturnsForDistributor(5L)).thenReturn(List.of(distributorReturn));

        var result = mockMvc.perform(get("/labels/1/distributors/5").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/detail"))
                .andExpect(model().attribute("label", label))
                .andExpect(model().attribute("distributor", distributor))
                .andReturn();

        @SuppressWarnings("unchecked")
        var sales = (List<Sale>) result.getModelAndView().getModel().get("sales");
        assertThat(sales).hasSize(1);
        assertThat(sales.get(0).id()).isEqualTo(10L);

        @SuppressWarnings("unchecked")
        var returns = (List<DistributorReturn>) result.getModelAndView().getModel().get("returns");
        assertThat(returns).hasSize(1);
        assertThat(returns.get(0).id()).isEqualTo(20L);
    }

    @Test
    void showDistributor_returnsNotFoundWhenDistributorBelongsToAnotherLabel() throws Exception {
        var label = LabelFactory.aLabel().id(1L).build();
        var distributorFromAnotherLabel = DistributorFactory.aDistributor()
                .id(99L).labelId(2L).build();

        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(99L)).thenReturn(Optional.of(distributorFromAnotherLabel));

        mockMvc.perform(get("/labels/1/distributors/99").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void showDistributor_returnsEmptyListsWhenNoActivity() throws Exception {
        var label = LabelFactory.aLabel().id(1L).build();
        var distributor = DistributorFactory.aDistributor().id(5L).labelId(1L).build();

        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(5L)).thenReturn(Optional.of(distributor));
        when(saleQueryApi.getSalesForDistributor(5L)).thenReturn(List.of());
        when(returnQueryApi.getReturnsForDistributor(5L)).thenReturn(List.of());

        mockMvc.perform(get("/labels/1/distributors/5").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/detail"))
                .andExpect(model().attribute("sales", List.of()))
                .andExpect(model().attribute("returns", List.of()));
    }
}
