package org.omt.labelmanager.catalog.label.application;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class DeleteLabelUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(DeleteLabelUseCase.class);

    private final LabelRepository repository;

    DeleteLabelUseCase(LabelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long id) {
        log.info("Deleting label with id {}", id);
        repository.deleteById(id);
    }
}
