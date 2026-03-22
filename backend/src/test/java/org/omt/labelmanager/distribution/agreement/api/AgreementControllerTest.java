package org.omt.labelmanager.distribution.agreement.api;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.LabelFactory;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.ReleaseFactory;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.agreement.CommissionType;
import org.omt.labelmanager.distribution.agreement.PricingAgreement;
import org.omt.labelmanager.distribution.distributor.DistributorFactory;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRunFactory;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AgreementController.class)
@Import(TestSecurityConfig.class)
class AgreementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgreementCommandApi commandApi;

    @MockitoBean
    private AgreementQueryApi queryApi;

    @MockitoBean
    private DistributorQueryApi distributorQueryApi;

    @MockitoBean
    private LabelQueryApi labelQueryApi;

    @MockitoBean
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @MockitoBean
    private ProductionRunQueryApi productionRunQueryApi;

    @MockitoBean
    private ReleaseQueryApi releaseQueryApi;

    private static final Long LABEL_ID = 1L;
    private static final Long DISTRIBUTOR_ID = 5L;
    private static final Long AGREEMENT_ID = 10L;
    private static final Long RUN_ID = 20L;

    @Test
    void listAgreements_redirectsToDistributorDetail() throws Exception {
        mockMvc.perform(get("/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/distributors/5"));
    }

    @Test
    void showCreateForm_returns200WithAvailableRuns() throws Exception {
        var label = LabelFactory.aLabel().id(LABEL_ID).build();
        var distributor = DistributorFactory.aDistributor().id(DISTRIBUTOR_ID).labelId(LABEL_ID).build();
        var run = ProductionRunFactory.aProductionRun().id(RUN_ID).build();
        var release = ReleaseFactory.aRelease().id(run.releaseId()).name("Test Album").build();

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));
        when(inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(DISTRIBUTOR_ID)).thenReturn(List.of(RUN_ID));
        when(queryApi.existsByDistributorIdAndProductionRunId(DISTRIBUTOR_ID, RUN_ID)).thenReturn(false);
        when(productionRunQueryApi.findById(RUN_ID)).thenReturn(Optional.of(run));
        when(releaseQueryApi.findById(run.releaseId())).thenReturn(Optional.of(release));

        mockMvc.perform(get("/labels/{labelId}/distributors/{distributorId}/agreements/new",
                        LABEL_ID, DISTRIBUTOR_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/agreement-form"))
                .andExpect(model().attributeExists("availableRuns", "form", "label", "distributor", "commissionTypes"));
    }

    @Test
    void createAgreement_withValidPercentageData_redirectsToDistributorDetail() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        when(commandApi.create(any(), any(), any(), any(), any())).thenReturn(agreement);

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .param("productionRunId", RUN_ID.toString())
                        .param("unitPrice", "9.99")
                        .param("commissionType", "PERCENTAGE")
                        .param("commissionValue", "15.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/distributors/5"));

        verify(commandApi).create(eq(DISTRIBUTOR_ID), eq(RUN_ID),
                eq(new BigDecimal("9.99")), eq(CommissionType.PERCENTAGE), eq(new BigDecimal("15.00")));
    }

    @Test
    void createAgreement_withFixedAmountAndPositiveValue_redirectsToDistributorDetail() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("9.99"), CommissionType.FIXED_AMOUNT, new BigDecimal("2.50"));
        when(commandApi.create(any(), any(), any(), any(), any())).thenReturn(agreement);

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .param("productionRunId", RUN_ID.toString())
                        .param("unitPrice", "9.99")
                        .param("commissionType", "FIXED_AMOUNT")
                        .param("commissionValue", "2.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/distributors/5"));

        verify(commandApi).create(eq(DISTRIBUTOR_ID), eq(RUN_ID),
                eq(new BigDecimal("9.99")), eq(CommissionType.FIXED_AMOUNT), eq(new BigDecimal("2.50")));
    }

    @Test
    void createAgreement_withDuplicateRun_reRendersFormWithError() throws Exception {
        var label = LabelFactory.aLabel().id(LABEL_ID).build();
        var distributor = DistributorFactory.aDistributor().id(DISTRIBUTOR_ID).labelId(LABEL_ID).build();

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));
        when(inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(DISTRIBUTOR_ID)).thenReturn(List.of());
        when(commandApi.create(any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateAgreementException(DISTRIBUTOR_ID, RUN_ID));

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .param("productionRunId", RUN_ID.toString())
                        .param("unitPrice", "9.99")
                        .param("commissionType", "PERCENTAGE")
                        .param("commissionValue", "15.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/agreement-form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void createAgreement_withInvalidUnitPrice_reRendersFormWithError() throws Exception {
        var label = LabelFactory.aLabel().id(LABEL_ID).build();
        var distributor = DistributorFactory.aDistributor().id(DISTRIBUTOR_ID).labelId(LABEL_ID).build();

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));
        when(inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(DISTRIBUTOR_ID)).thenReturn(List.of());
        when(commandApi.create(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Unit price must be greater than zero"));

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .param("productionRunId", RUN_ID.toString())
                        .param("unitPrice", "0.00")
                        .param("commissionType", "PERCENTAGE")
                        .param("commissionValue", "15.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/agreement-form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void createAgreement_withPercentageOver100_reRendersFormWithError() throws Exception {
        var label = LabelFactory.aLabel().id(LABEL_ID).build();
        var distributor = DistributorFactory.aDistributor().id(DISTRIBUTOR_ID).labelId(LABEL_ID).build();

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));
        when(inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(DISTRIBUTOR_ID)).thenReturn(List.of());
        when(commandApi.create(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Commission percentage must be between 0 and 100"));

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .param("productionRunId", RUN_ID.toString())
                        .param("unitPrice", "9.99")
                        .param("commissionType", "PERCENTAGE")
                        .param("commissionValue", "101.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/agreement-form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void createAgreement_withFixedAmountAndZeroValue_reRendersFormWithError() throws Exception {
        var label = LabelFactory.aLabel().id(LABEL_ID).build();
        var distributor = DistributorFactory.aDistributor().id(DISTRIBUTOR_ID).labelId(LABEL_ID).build();

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));
        when(inventoryMovementQueryApi.getProductionRunIdsAllocatedToDistributor(DISTRIBUTOR_ID)).thenReturn(List.of());
        when(commandApi.create(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Commission value must be greater than zero"));

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .param("productionRunId", RUN_ID.toString())
                        .param("unitPrice", "9.99")
                        .param("commissionType", "FIXED_AMOUNT")
                        .param("commissionValue", "0.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/agreement-form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void showEditForm_returns200WithPopulatedForm() throws Exception {
        var label = LabelFactory.aLabel().id(LABEL_ID).build();
        var distributor = DistributorFactory.aDistributor().id(DISTRIBUTOR_ID).labelId(LABEL_ID).build();
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        var run = ProductionRunFactory.aProductionRun().id(RUN_ID).build();
        var release = ReleaseFactory.aRelease().id(run.releaseId()).name("Test Album").build();

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
        when(productionRunQueryApi.findById(RUN_ID)).thenReturn(Optional.of(run));
        when(releaseQueryApi.findById(run.releaseId())).thenReturn(Optional.of(release));

        mockMvc.perform(get("/labels/{labelId}/distributors/{distributorId}/agreements/{id}/edit",
                        LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("distributor/agreement-form"))
                .andExpect(model().attributeExists("agreement", "form", "productionRunDisplayName", "commissionTypes"));
    }

    @Test
    void showEditForm_whenAgreementBelongsToDifferentDistributor_throwsNotFound() {
        var otherDistributorId = 99L;
        var agreement = agreement(AGREEMENT_ID, otherDistributorId, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        var label = LabelFactory.aLabel().id(LABEL_ID).build();
        var distributor = DistributorFactory.aDistributor().id(DISTRIBUTOR_ID).labelId(LABEL_ID).build();

        when(labelQueryApi.findById(LABEL_ID)).thenReturn(Optional.of(label));
        when(distributorQueryApi.findById(DISTRIBUTOR_ID)).thenReturn(Optional.of(distributor));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        assertThatThrownBy(() ->
                mockMvc.perform(get("/labels/{labelId}/distributors/{distributorId}/agreements/{id}/edit",
                                LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)))
                .hasRootCauseInstanceOf(AgreementNotFoundException.class);
    }

    @Test
    void updateAgreement_withValidData_redirectsToDistributorDetail() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("12.00"), CommissionType.PERCENTAGE, new BigDecimal("20.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
        when(commandApi.update(any(), any(), any(), any())).thenReturn(agreement);

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements/{id}",
                        LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)
                        .param("productionRunId", RUN_ID.toString())
                        .param("unitPrice", "12.00")
                        .param("commissionType", "PERCENTAGE")
                        .param("commissionValue", "20.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/distributors/5"));

        verify(commandApi).update(eq(AGREEMENT_ID), eq(new BigDecimal("12.00")),
                eq(CommissionType.PERCENTAGE), eq(new BigDecimal("20.00")));
    }

    @Test
    void updateAgreement_whenAgreementBelongsToDifferentDistributor_throwsNotFound() {
        var otherDistributorId = 99L;
        var agreement = agreement(AGREEMENT_ID, otherDistributorId, RUN_ID,
                new BigDecimal("12.00"), CommissionType.PERCENTAGE, new BigDecimal("20.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        assertThatThrownBy(() ->
                mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements/{id}",
                                LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)
                                .param("unitPrice", "12.00")
                                .param("commissionType", "PERCENTAGE")
                                .param("commissionValue", "20.00")))
                .hasRootCauseInstanceOf(AgreementNotFoundException.class);
    }

    @Test
    void deleteAgreement_deletesAndRedirectsToDistributorDetail() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements/{id}/delete",
                        LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/distributors/5"));

        verify(commandApi).delete(AGREEMENT_ID);
    }

    @Test
    void deleteAgreement_whenAgreementBelongsToDifferentDistributor_throwsNotFound() {
        var otherDistributorId = 99L;
        var agreement = agreement(AGREEMENT_ID, otherDistributorId, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        assertThatThrownBy(() ->
                mockMvc.perform(post("/labels/{labelId}/distributors/{distributorId}/agreements/{id}/delete",
                                LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)))
                .hasRootCauseInstanceOf(AgreementNotFoundException.class);
    }

    private PricingAgreement agreement(Long id, Long distributorId, Long productionRunId,
            BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue) {
        return new PricingAgreement(id, distributorId, productionRunId,
                unitPrice, commissionType, commissionValue, Instant.now());
    }
}
