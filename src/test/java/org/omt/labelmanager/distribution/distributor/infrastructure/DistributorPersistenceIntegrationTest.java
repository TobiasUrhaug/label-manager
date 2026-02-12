package org.omt.labelmanager.distribution.distributor.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class DistributorPersistenceIntegrationTest extends AbstractIntegrationTest {

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
    void savesAndRetrievesDistributor() {
        var entity = new DistributorEntity(labelId, "Bandcamp", ChannelType.DIRECT);

        var saved = distributorRepository.save(entity);

        var retrieved = distributorRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getLabelId()).isEqualTo(labelId);
        assertThat(retrieved.get().getName()).isEqualTo("Bandcamp");
        assertThat(retrieved.get().getChannelType()).isEqualTo(ChannelType.DIRECT);
    }

    @Test
    void findsByLabelId() {
        distributorRepository.save(
                new DistributorEntity(labelId, "Direct Sales", ChannelType.DIRECT));
        distributorRepository.save(
                new DistributorEntity(labelId, "Cargo Records", ChannelType.DISTRIBUTOR));

        var otherLabel = labelTestHelper.createLabel("Other Label");
        distributorRepository.save(
                new DistributorEntity(otherLabel.id(), "Record Shop", ChannelType.RECORD_STORE));

        var channelsForLabel = distributorRepository.findByLabelId(labelId);

        assertThat(channelsForLabel).hasSize(2);
        assertThat(channelsForLabel)
                .allMatch(channel -> channel.getLabelId().equals(labelId));
    }

    // Note: Cascade delete test removed as LabelRepository is package-private.
    // Cascade behavior from Label to Distributor is tested in the label package.

    @Test
    void findsByLabelIdAndChannelType() {
        distributorRepository.save(
                new DistributorEntity(labelId, "Direct Sales", ChannelType.DIRECT));
        distributorRepository.save(
                new DistributorEntity(labelId, "Cargo Records", ChannelType.DISTRIBUTOR));

        var directDistributor =
                distributorRepository.findByLabelIdAndChannelType(labelId, ChannelType.DIRECT);

        assertThat(directDistributor).isPresent();
        assertThat(directDistributor.get().getName()).isEqualTo("Direct Sales");
        assertThat(directDistributor.get().getChannelType()).isEqualTo(ChannelType.DIRECT);
    }

    @Test
    void findByLabelIdAndChannelType_returnsEmpty_whenNotFound() {
        distributorRepository.save(
                new DistributorEntity(labelId, "Cargo Records", ChannelType.DISTRIBUTOR));

        var directDistributor =
                distributorRepository.findByLabelIdAndChannelType(labelId, ChannelType.DIRECT);

        assertThat(directDistributor).isEmpty();
    }
}
