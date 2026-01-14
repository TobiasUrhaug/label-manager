package org.omt.labelmanager.label;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LabelCRUDHandler {

    private final LabelRepository repository;

    public LabelCRUDHandler(LabelRepository repository) {
        this.repository = repository;
    }

    public List<Label> getAllLabels() {
        return repository.findAll().stream().map(Label::fromEntity).toList();
    }

    public void createLabel(String labelName) {
        repository.save(new LabelEntity(labelName));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<Label> findById(long id) {
        return repository.findById(id).map(Label::fromEntity);
    }
}
