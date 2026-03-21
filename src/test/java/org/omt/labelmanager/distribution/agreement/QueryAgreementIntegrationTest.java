package org.omt.labelmanager.distribution.agreement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.agreement.domain.CommissionType;
import org.omt.labelmanager.distribution.agreement.infrastructure.PricingAgreementRepository;
import org.omt.labelmanager.distribution.distributor.DistributorTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryAgreementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AgreementCommandApi agreementCommandApi;

    @Autowired
    private AgreementQueryApi agreementQueryApi;

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
    void findById_returnsAgreement_whenItExists() {
        var created = agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("10.00"));

        var found = agreementQueryApi.findById(created.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(created.id());
    }

    @Test
    void findById_returnsEmpty_whenAgreementDoesNotExist() {
        assertThat(agreementQueryApi.findById(999L)).isEmpty();
    }

    @Test
    void findByDistributorId_returnsAllAgreementsForDistributor() {
        agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("10.00"));

        var agreements = agreementQueryApi.findByDistributorId(distributorId);

        assertThat(agreements).hasSize(1);
        assertThat(agreements.get(0).distributorId()).isEqualTo(distributorId);
    }

    @Test
    void findByDistributorId_returnsEmpty_whenNoAgreementsExist() {
        assertThat(agreementQueryApi.findByDistributorId(distributorId)).isEmpty();
    }

    @Test
    void existsByDistributorIdAndProductionRunId_returnsTrue_whenAgreementExists() {
        agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), CommissionType.PERCENTAGE, new BigDecimal("10.00"));

        assertThat(agreementQueryApi.existsByDistributorIdAndProductionRunId(distributorId, productionRunId)).isTrue();
    }

    @Test
    void existsByDistributorIdAndProductionRunId_returnsFalse_whenNoAgreementExists() {
        assertThat(agreementQueryApi.existsByDistributorIdAndProductionRunId(distributorId, productionRunId)).isFalse();
    }
}
