package org.omt.labelmanager.distribution.agreement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.api.DuplicateAgreementException;
import org.omt.labelmanager.distribution.agreement.CommissionType;
import org.omt.labelmanager.distribution.agreement.persistence.PricingAgreementRepository;
import org.omt.labelmanager.distribution.distributor.DistributorTestHelper;
import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CreateAgreementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AgreementCommandApi agreementCommandApi;

    @Autowired
    private PricingAgreementRepository repository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private DistributorTestHelper distributorTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private ProductionRunTestHelper productionRunTestHelper;

    private Long distributorId;
    private Long productionRunId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        var distributor = distributorTestHelper.createDistributor(label.id(), "Test Distributor", ChannelType.DISTRIBUTOR);
        distributorId = distributor.id();

        Long releaseId = releaseTestHelper.createReleaseEntity("Test Album", label.id());
        var run = productionRunTestHelper.createProductionRun(releaseId, ReleaseFormat.VINYL, 100);
        productionRunId = run.id();
    }

    @Test
    void create_persistsAgreementWithAllFields() {
        var agreement = agreementCommandApi.create(
                distributorId,
                productionRunId,
                new BigDecimal("12.50"),
                CommissionType.PERCENTAGE,
                new BigDecimal("15.00")
        );

        assertThat(agreement.id()).isNotNull();
        assertThat(agreement.distributorId()).isEqualTo(distributorId);
        assertThat(agreement.productionRunId()).isEqualTo(productionRunId);
        assertThat(agreement.unitPrice()).isEqualByComparingTo("12.50");
        assertThat(agreement.commissionType()).isEqualTo(CommissionType.PERCENTAGE);
        assertThat(agreement.commissionValue()).isEqualByComparingTo("15.00");
        assertThat(agreement.createdAt()).isNotNull();
    }

    @Test
    void create_throwsDuplicateAgreementException_whenAgreementAlreadyExists() {
        agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("12.50"), CommissionType.PERCENTAGE, new BigDecimal("15.00"));

        assertThatThrownBy(() ->
                agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("20.00"))
        ).isInstanceOf(DuplicateAgreementException.class);
    }

    @Test
    void create_throwsIllegalArgumentException_whenUnitPriceIsZero() {
        assertThatThrownBy(() ->
                agreementCommandApi.create(distributorId, productionRunId, BigDecimal.ZERO, CommissionType.PERCENTAGE, new BigDecimal("15.00"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throwsIllegalArgumentException_whenUnitPriceIsNegative() {
        assertThatThrownBy(() ->
                agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("-1.00"), CommissionType.PERCENTAGE, new BigDecimal("15.00"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throwsIllegalArgumentException_whenCommissionExceedsHundred() {
        assertThatThrownBy(() ->
                agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("12.50"), CommissionType.PERCENTAGE, new BigDecimal("100.01"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_allowsZeroPercentageCommission() {
        var agreement = agreementCommandApi.create(
                distributorId, productionRunId, new BigDecimal("12.50"), CommissionType.PERCENTAGE, BigDecimal.ZERO
        );

        assertThat(agreement.commissionValue()).isEqualByComparingTo("0");
    }

    @Test
    void create_allowsHundredPercentCommission() {
        var agreement = agreementCommandApi.create(
                distributorId, productionRunId, new BigDecimal("12.50"), CommissionType.PERCENTAGE, new BigDecimal("100.00")
        );

        assertThat(agreement.commissionValue()).isEqualByComparingTo("100.00");
    }
}
