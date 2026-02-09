package org.omt.labelmanager.catalog.label;

import org.omt.labelmanager.catalog.label.api.LabelQueryFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class LabelQueryHandler implements LabelQueryFacade {

    private static final Logger log = LoggerFactory.getLogger(LabelQueryHandler.class);

    private final LabelRepository repository;

    LabelQueryHandler(LabelRepository repository) {
        this.repository = repository;
    }

    public Optional<Label> findById(Long id) {
        return repository.findById(id).map(Label::fromEntity);
    }

    public boolean exists(Long id) {
        return repository.existsById(id);
    }

    public List<Label> getLabelsForUser(Long userId) {
        List<Label> labels = repository.findByUserId(userId).stream()
                .map(Label::fromEntity)
                .toList();
        log.debug("Retrieved {} labels for user {}", labels.size(), userId);
        return labels;
    }
}
