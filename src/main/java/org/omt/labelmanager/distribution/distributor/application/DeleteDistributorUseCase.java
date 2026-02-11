package org.omt.labelmanager.distribution.distributor.application;

import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteDistributorUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(DeleteDistributorUseCase.class);

    private final DistributorRepository repository;

    DeleteDistributorUseCase(DistributorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean execute(Long id) {
        if (!repository.existsById(id)) {
            log.warn("Distributor with id {} not found for deletion", id);
            return false;
        }

        repository.deleteById(id);
        log.info("Deleted distributor with id {}", id);
        return true;
    }
}
