package org.omt.labelmanager.distribution.agreement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementNotFoundException;
import org.omt.labelmanager.distribution.agreement.CommissionType;
import org.omt.labelmanager.distribution.agreement.persistence.PricingAgreementRepository;
import org.omt.labelmanager.distribution.distributor.DistributorTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UpdateAgreementIntegrationTest extends AbstractIntegrationTest {

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
    void update_changesUnitPriceAndCommission() {
        var created = agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("10.00"));

        var updated = agreementCommandApi.update(created.id(), new BigDecimal("20.00"), CommissionType.PERCENTAGE, new BigDecimal("25.00"));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.unitPrice()).isEqualByComparingTo("20.00");
        assertThat(updated.commissionValue()).isEqualByComparingTo("25.00");
    }

    @Test
    void update_throwsAgreementNotFoundException_whenAgreementDoesNotExist() {
        assertThatThrownBy(() ->
                agreementCommandApi.update(999L, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("10.00"))
        ).isInstanceOf(AgreementNotFoundException.class);
    }

    @Test
    void update_throwsIllegalArgumentException_whenUnitPriceIsZero() {
        var created = agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("10.00"));

        assertThatThrownBy(() ->
                agreementCommandApi.update(created.id(), BigDecimal.ZERO, CommissionType.PERCENTAGE, new BigDecimal("10.00"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_throwsIllegalArgumentException_whenCommissionExceedsHundred() {
        var created = agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("10.00"));

        assertThatThrownBy(() ->
                agreementCommandApi.update(created.id(), new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("100.01"))
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
