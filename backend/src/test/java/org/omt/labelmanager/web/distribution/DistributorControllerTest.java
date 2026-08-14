package org.omt.labelmanager.web.distribution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.distributor.DistributorFactory;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.identity.api.user.AppUserDetails;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.distributorreturn.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.omt.labelmanager.web.LabelScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DistributorController.class)
@Import(TestSecurityConfig.class)
class DistributorControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private DistributorCommandApi distributorCRUDHandler;

    @MockitoBean private DistributorQueryApi distributorQueryApi;

    @MockitoBean private LabelQueryApi labelQueryApi;

    @MockitoBean private LabelScope labelScope;

    @MockitoBean private SaleQueryApi saleQueryApi;

    @MockitoBean private DistributorReturnQueryApi returnQueryApi;

    @MockitoBean private AgreementQueryApi agreementQueryApi;

    @MockitoBean private ProductionRunQueryApi productionRunQueryApi;

    @MockitoBean private org.omt.labelmanager.catalog.release.api.ReleaseQueryApi releaseQueryApi;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void addDistributor_callsHandlerAndReturnsCreated() throws Exception {
        mockMvc.perform(
                        post("/api/labels/1/distributors")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Bandcamp", "channelType": "DIRECT"}
                                """))
                .andExpect(status().isCreated());

        verify(distributorCRUDHandler)
                .createDistributor(eq(1L), eq("Bandcamp"), eq(ChannelType.DIRECT));
    }

    @Test
    void addDistributor_worksWithDistributorType() throws Exception {
        mockMvc.perform(
                        post("/api/labels/1/distributors")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Cargo Records", "channelType": "DISTRIBUTOR"}
                                """))
                .andExpect(status().isCreated());

        verify(distributorCRUDHandler)
                .createDistributor(eq(1L), eq("Cargo Records"), eq(ChannelType.DISTRIBUTOR));
    }

    @Test
    void addDistributor_worksWithRecordStoreType() throws Exception {
        mockMvc.perform(
                        post("/api/labels/1/distributors")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Local Record Shop", "channelType": "RECORD_STORE"}
                                """))
                .andExpect(status().isCreated());

        verify(distributorCRUDHandler)
                .createDistributor(eq(1L), eq("Local Record Shop"), eq(ChannelType.RECORD_STORE));
    }

    @Test
    void deleteDistributor_callsHandlerAndReturnsNoContent() throws Exception {
        when(labelQueryApi.findById(1L))
                .thenReturn(Optional.of(LabelFactory.aLabel().id(1L).build()));
        when(distributorQueryApi.findById(99L))
                .thenReturn(
                        Optional.of(DistributorFactory.aDistributor().id(99L).labelId(1L).build()));
        when(distributorCRUDHandler.delete(99L)).thenReturn(true);

        mockMvc.perform(delete("/api/labels/1/distributors/99").with(user(testUser)).with(csrf()))
                .andExpect(status().isNoContent());

        verify(distributorCRUDHandler).delete(99L);
    }

    @Test
    void distributors_returnsTheLabelsDistributors() throws Exception {
        var label = LabelFactory.aLabel().id(1L).name("My Label").build();
        var distributor =
                DistributorFactory.aDistributor().id(5L).labelId(1L).name("Alpha").build();
        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findByLabelId(1L)).thenReturn(List.of(distributor));

        mockMvc.perform(get("/api/labels/1/distributors").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Alpha"));
    }

    @Test
    void showDistributor_returnsTheDistributor() throws Exception {
        var label = LabelFactory.aLabel().id(1L).name("My Label").build();
        var distributor =
                DistributorFactory.aDistributor()
                        .id(5L)
                        .labelId(1L)
                        .name("Cargo Records")
                        .channelType(ChannelType.DISTRIBUTOR)
                        .build();

        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(5L)).thenReturn(Optional.of(distributor));

        mockMvc.perform(get("/api/labels/1/distributors/5").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cargo Records"))
                .andExpect(jsonPath("$.sales").doesNotExist())
                .andExpect(jsonPath("$.returns").doesNotExist())
                .andExpect(jsonPath("$.agreements").doesNotExist());
    }

    @Test
    void showDistributor_returnsNotFoundWhenDistributorBelongsToAnotherLabel() throws Exception {
        var label = LabelFactory.aLabel().id(1L).build();
        var distributorFromAnotherLabel =
                DistributorFactory.aDistributor().id(99L).labelId(2L).build();

        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(99L))
                .thenReturn(Optional.of(distributorFromAnotherLabel));

        mockMvc.perform(get("/api/labels/1/distributors/99").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void agreements_returnsTheDistributorsAgreements() throws Exception {
        var label = LabelFactory.aLabel().id(1L).build();
        var distributor = DistributorFactory.aDistributor().id(5L).labelId(1L).build();

        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(5L)).thenReturn(Optional.of(distributor));
        when(agreementQueryApi.findByDistributorId(5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/labels/1/distributors/5/agreements").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void agreements_returnsNotFoundWhenDistributorBelongsToAnotherLabel() throws Exception {
        var label = LabelFactory.aLabel().id(1L).build();
        when(labelQueryApi.findById(1L)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(99L))
                .thenReturn(
                        Optional.of(DistributorFactory.aDistributor().id(99L).labelId(2L).build()));

        mockMvc.perform(get("/api/labels/1/distributors/99/agreements").with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDistributor_returns404WhenItBelongsToAnotherLabel() throws Exception {
        when(labelQueryApi.findById(1L))
                .thenReturn(Optional.of(LabelFactory.aLabel().id(1L).build()));
        when(distributorQueryApi.findById(99L))
                .thenReturn(
                        Optional.of(DistributorFactory.aDistributor().id(99L).labelId(2L).build()));

        mockMvc.perform(delete("/api/labels/1/distributors/99").with(user(testUser)).with(csrf()))
                .andExpect(status().isNotFound());

        verify(distributorCRUDHandler, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void addDistributor_returns404WhenTheLabelDoesNotExist() throws Exception {
        doThrow(new EntityNotFoundException("Label not found: 999"))
                .when(labelScope)
                .requireLabel(999L);

        mockMvc.perform(
                        post("/api/labels/999/distributors")
                                .with(user(testUser))
                                .with(csrf())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {"name": "Orphan", "channelType": "DISTRIBUTOR"}
                                """))
                .andExpect(status().isNotFound());

        verify(distributorCRUDHandler, org.mockito.Mockito.never())
                .createDistributor(any(), any(), any());
    }
}
