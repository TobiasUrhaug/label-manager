package org.omt.labelmanager.label;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LabelService {

    private final LabelRepository repository;

    public LabelService(LabelRepository repository) {
        this.repository = repository;
    }

    public List<Label> getAllLabels() {
        return repository.findAll();

    }

    public void createLabel(String labelName) {
        repository.save(new Label(labelName));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<Label> findById(long id) {
        return repository.findById(id);
    }
}
