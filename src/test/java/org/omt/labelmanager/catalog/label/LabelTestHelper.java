package org.omt.labelmanager.catalog.label;

import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.label.infrastructure.LabelEntity;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test label data.
 * Used by integration tests in other modules that need label fixtures.
 *
 * Note: This helper bypasses the API for simplicity. If your test needs
 * a DIRECT distributor, use createLabelWithDirectDistributor() or create
 * it manually.
 */
@Component
public class LabelTestHelper {

    private final LabelRepository labelRepository;
    private final DistributorRepository distributorRepository;

    public LabelTestHelper(
            LabelRepository labelRepository,
            DistributorRepository distributorRepository
    ) {
        this.labelRepository = labelRepository;
        this.distributorRepository = distributorRepository;
    }

    public Label createLabel(String name) {
        LabelEntity entity = new LabelEntity(name, null, null);
        entity = labelRepository.save(entity);
        return Label.fromEntity(entity);
    }

    public Label createLabel(String name, String email, String website) {
        LabelEntity entity = new LabelEntity(name, email, website);
        entity = labelRepository.save(entity);
        return Label.fromEntity(entity);
    }

    public Label createLabel(String name, Long userId) {
        LabelEntity entity = new LabelEntity(name, null, null);
        entity.setUserId(userId);
        entity = labelRepository.save(entity);
        return Label.fromEntity(entity);
    }

    /**
     * Creates a label with a DIRECT distributor (for tests that need it).
     * Use this when testing sales or inventory management.
     */
    public Label createLabelWithDirectDistributor(String name) {
        var label = createLabel(name);

        distributorRepository.save(
                new DistributorEntity(
                        label.id(),
                        name + " Direct Sales",
                        ChannelType.DIRECT
                )
        );

        return label;
    }
}
