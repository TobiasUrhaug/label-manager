package org.omt.labelmanager.distribution.distributor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateDistributorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DistributorCommandApi distributorCommandApi;

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
    void createDistributor_persistsDistributorWithAllFields() {
        var distributor = distributorCommandApi.createDistributor(
                labelId,
                "Bandcamp",
                ChannelType.DIRECT
        );

        assertThat(distributor.id()).isNotNull();
        assertThat(distributor.labelId()).isEqualTo(labelId);
        assertThat(distributor.name()).isEqualTo("Bandcamp");
        assertThat(distributor.channelType()).isEqualTo(ChannelType.DIRECT);
    }

    @Test
    void createDistributor_worksWithDistributorType() {
        var distributor = distributorCommandApi.createDistributor(
                labelId,
                "Cargo Records",
                ChannelType.DISTRIBUTOR
        );

        assertThat(distributor.channelType()).isEqualTo(ChannelType.DISTRIBUTOR);
    }

    @Test
    void createDistributor_worksWithRetailType() {
        var distributor = distributorCommandApi.createDistributor(
                labelId,
                "Local Record Shop",
                ChannelType.RETAIL
        );

        assertThat(distributor.channelType()).isEqualTo(ChannelType.RETAIL);
    }
}
