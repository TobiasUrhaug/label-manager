package org.omt.labelmanager.distribution.agreement.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.agreement.CommissionType;
import org.omt.labelmanager.distribution.agreement.PricingAgreement;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.identity.application.AppUserDetails;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    private static final Long LABEL_ID = 1L;
    private static final Long DISTRIBUTOR_ID = 5L;
    private static final Long AGREEMENT_ID = 10L;
    private static final Long RUN_ID = 20L;

    @Test
    void createAgreement_withValidData_returnsCreated() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        when(commandApi.create(any(), any(), any(), any(), any())).thenReturn(agreement);

        mockMvc.perform(post("/api/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productionRunId": 20,
                                  "unitPrice": 9.99,
                                  "commissionType": "PERCENTAGE",
                                  "commissionValue": 15.00
                                }
                                """))
                .andExpect(status().isCreated());

        verify(commandApi).create(eq(DISTRIBUTOR_ID), eq(RUN_ID),
                eq(new BigDecimal("9.99")), eq(CommissionType.PERCENTAGE), eq(new BigDecimal("15.00")));
    }

    @Test
    void createAgreement_withFixedAmount_returnsCreated() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("9.99"), CommissionType.FIXED_AMOUNT, new BigDecimal("2.50"));
        when(commandApi.create(any(), any(), any(), any(), any())).thenReturn(agreement);

        mockMvc.perform(post("/api/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productionRunId": 20,
                                  "unitPrice": 9.99,
                                  "commissionType": "FIXED_AMOUNT",
                                  "commissionValue": 2.50
                                }
                                """))
                .andExpect(status().isCreated());

        verify(commandApi).create(eq(DISTRIBUTOR_ID), eq(RUN_ID),
                eq(new BigDecimal("9.99")), eq(CommissionType.FIXED_AMOUNT), eq(new BigDecimal("2.50")));
    }

    @Test
    void createAgreement_withDuplicateRun_returnsBadRequest() throws Exception {
        when(commandApi.create(any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateAgreementException(DISTRIBUTOR_ID, RUN_ID));

        mockMvc.perform(post("/api/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productionRunId": 20,
                                  "unitPrice": 9.99,
                                  "commissionType": "PERCENTAGE",
                                  "commissionValue": 15.00
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAgreement_withInvalidArgument_returnsBadRequest() throws Exception {
        when(commandApi.create(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Unit price must be greater than zero"));

        mockMvc.perform(post("/api/labels/{labelId}/distributors/{distributorId}/agreements",
                        LABEL_ID, DISTRIBUTOR_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productionRunId": 20,
                                  "unitPrice": 0.00,
                                  "commissionType": "PERCENTAGE",
                                  "commissionValue": 15.00
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAgreement_withValidData_returnsNoContent() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("12.00"), CommissionType.PERCENTAGE, new BigDecimal("20.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));
        when(commandApi.update(any(), any(), any(), any())).thenReturn(agreement);

        mockMvc.perform(put("/api/labels/{labelId}/distributors/{distributorId}/agreements/{id}",
                        LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "unitPrice": 12.00,
                                  "commissionType": "PERCENTAGE",
                                  "commissionValue": 20.00
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(commandApi).update(eq(AGREEMENT_ID), eq(new BigDecimal("12.00")),
                eq(CommissionType.PERCENTAGE), eq(new BigDecimal("20.00")));
    }

    @Test
    void updateAgreement_whenAgreementBelongsToDifferentDistributor_returnsNotFound() throws Exception {
        var otherDistributorId = 99L;
        var agreement = agreement(AGREEMENT_ID, otherDistributorId, RUN_ID,
                new BigDecimal("12.00"), CommissionType.PERCENTAGE, new BigDecimal("20.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        mockMvc.perform(put("/api/labels/{labelId}/distributors/{distributorId}/agreements/{id}",
                        LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)
                        .with(user(testUser))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "unitPrice": 12.00,
                                  "commissionType": "PERCENTAGE",
                                  "commissionValue": 20.00
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAgreement_returnsNoContent() throws Exception {
        var agreement = agreement(AGREEMENT_ID, DISTRIBUTOR_ID, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        mockMvc.perform(delete("/api/labels/{labelId}/distributors/{distributorId}/agreements/{id}",
                        LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(commandApi).delete(AGREEMENT_ID);
    }

    @Test
    void deleteAgreement_whenAgreementBelongsToDifferentDistributor_returnsNotFound() throws Exception {
        var otherDistributorId = 99L;
        var agreement = agreement(AGREEMENT_ID, otherDistributorId, RUN_ID,
                new BigDecimal("9.99"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));
        when(queryApi.findById(AGREEMENT_ID)).thenReturn(Optional.of(agreement));

        mockMvc.perform(delete("/api/labels/{labelId}/distributors/{distributorId}/agreements/{id}",
                        LABEL_ID, DISTRIBUTOR_ID, AGREEMENT_ID)
                        .with(user(testUser))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    private PricingAgreement agreement(Long id, Long distributorId, Long productionRunId,
            BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue) {
        return new PricingAgreement(id, distributorId, productionRunId,
                unitPrice, commissionType, commissionValue, Instant.now());
    }
}
