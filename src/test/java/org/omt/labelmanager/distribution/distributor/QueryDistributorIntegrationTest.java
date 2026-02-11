package org.omt.labelmanager.distribution.distributor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryDistributorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DistributorCommandApi distributorCommandApi;

    @Autowired
    private DistributorQueryApi distributorQueryApi;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    private Long labelId;

    @BeforeEach
    void setUp() {
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        labelId = label.id();
    }

    @Test
    void findByLabelId_returnsAllDistributorsForLabel() {
        distributorCommandApi.createDistributor(labelId, "Direct Sales", ChannelType.DIRECT);
        distributorCommandApi.createDistributor(labelId, "Cargo Records", ChannelType.DISTRIBUTOR);

        var distributors = distributorQueryApi.findByLabelId(labelId);

        assertThat(distributors).hasSize(2);
        assertThat(distributors).extracting("name")
                .containsExactlyInAnyOrder("Direct Sales", "Cargo Records");
    }

    @Test
    void findByLabelId_returnsEmptyListWhenNoDistributors() {
        var distributors = distributorQueryApi.findByLabelId(labelId);

        assertThat(distributors).isEmpty();
    }

    @Test
    void findByLabelId_doesNotReturnDistributorsFromOtherLabels() {
        var otherLabel = labelTestHelper.createLabel("Other Label");
        distributorCommandApi.createDistributor(otherLabel.id(), "Other Distributor", ChannelType.DIRECT);
        distributorCommandApi.createDistributor(labelId, "My Distributor", ChannelType.DIRECT);

        var distributors = distributorQueryApi.findByLabelId(labelId);

        assertThat(distributors).hasSize(1);
        assertThat(distributors.get(0).name()).isEqualTo("My Distributor");
    }
}
