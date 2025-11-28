package org.omt.LabelManager.label;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabelService {

    private final LabelRepository repository;

    public LabelService(LabelRepository repository) {
        this.repository = repository;
    }

    public List<String> getAllLabels() {
        return repository
                .findAll()
                .stream()
                .map(Label::getName)
                .toList();
    }

    public void createLabel(String labelName) {
        repository.save(new Label(labelName));
    }
}
