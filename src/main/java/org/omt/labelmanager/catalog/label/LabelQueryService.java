package org.omt.labelmanager.catalog.label;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LabelQueryService {

    private final LabelRepository repository;

    public LabelQueryService(LabelRepository repository) {
        this.repository = repository;
    }

    public Optional<Label> findById(Long id) {
        return repository.findById(id).map(Label::fromEntity);
    }

    public boolean exists(Long id) {
        return repository.existsById(id);
    }
}
