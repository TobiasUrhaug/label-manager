package org.omt.labelmanager.distribution.agreement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementNotFoundException;
import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.agreement.infrastructure.PricingAgreementRepository;
import org.omt.labelmanager.distribution.distributor.DistributorTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeleteAgreementIntegrationTest extends AbstractIntegrationTest {

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
    void delete_removesAgreementFromDatabase() {
        var agreement = agreementCommandApi.create(distributorId, productionRunId, new BigDecimal("10.00"), new BigDecimal("10.00"));

        agreementCommandApi.delete(agreement.id());

        assertThat(agreementQueryApi.findById(agreement.id())).isEmpty();
    }

    @Test
    void delete_throwsAgreementNotFoundException_whenAgreementDoesNotExist() {
        assertThatThrownBy(() ->
                agreementCommandApi.delete(999L)
        ).isInstanceOf(AgreementNotFoundException.class);
    }
}
