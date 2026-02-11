package org.omt.labelmanager.inventory.productionrun.application;

import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteProductionRunUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(DeleteProductionRunUseCase.class);

    private final ProductionRunRepository repository;

    DeleteProductionRunUseCase(ProductionRunRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean execute(Long id) {
        if (!repository.existsById(id)) {
            log.warn("Production run with id {} not found for deletion", id);
            return false;
        }

        repository.deleteById(id);
        log.info("Deleted production run with id {}", id);
        return true;
    }
}
