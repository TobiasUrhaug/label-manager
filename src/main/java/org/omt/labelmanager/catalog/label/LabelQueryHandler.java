package org.omt.labelmanager.catalog.label;

import org.omt.labelmanager.catalog.label.api.LabelQueryFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class LabelQueryHandler implements LabelQueryFacade {

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
}
