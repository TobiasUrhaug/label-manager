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

public class DeleteDistributorIntegrationTest extends AbstractIntegrationTest {

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
    void delete_removesDistributorFromDatabase() {
        var distributor = distributorCommandApi.createDistributor(
                labelId,
                "Bandcamp",
                ChannelType.DIRECT
        );

        boolean deleted = distributorCommandApi.delete(distributor.id());

        assertThat(deleted).isTrue();
        assertThat(distributorQueryApi.findByLabelId(labelId)).isEmpty();
    }

    @Test
    void delete_returnsFalseForNonExistentDistributor() {
        boolean deleted = distributorCommandApi.delete(999L);

        assertThat(deleted).isFalse();
    }
}
