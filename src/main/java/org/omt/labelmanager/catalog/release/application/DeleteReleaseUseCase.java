package org.omt.labelmanager.catalog.release.application;

import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteReleaseUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(DeleteReleaseUseCase.class);

    private final ReleaseRepository releaseRepository;

    DeleteReleaseUseCase(ReleaseRepository releaseRepository) {
        this.releaseRepository = releaseRepository;
    }

    @Transactional
    public void execute(Long id) {
        log.info("Deleting release with id {}", id);
        releaseRepository.deleteById(id);
    }
}
