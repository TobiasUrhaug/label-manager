package org.omt.labelmanager.catalog.label;

import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.label.infrastructure.LabelEntity;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.springframework.stereotype.Component;

/**
 * Public helper for creating test label data.
 * Used by integration tests in other modules that need label fixtures.
 */
@Component
public class LabelTestHelper {

    private final LabelRepository labelRepository;

    public LabelTestHelper(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
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
}
