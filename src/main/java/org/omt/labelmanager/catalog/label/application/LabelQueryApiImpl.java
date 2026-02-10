package org.omt.labelmanager.catalog.label.application;

import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class LabelQueryApiImpl implements LabelQueryApi {

    private static final Logger log =
            LoggerFactory.getLogger(LabelQueryApiImpl.class);

    private final LabelRepository repository;

    LabelQueryApiImpl(LabelRepository repository) {
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
