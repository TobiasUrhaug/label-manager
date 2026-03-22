package org.omt.labelmanager.catalog.artist.application;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class DeleteArtistUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteArtistUseCase.class);

    private final ArtistRepository repository;

    DeleteArtistUseCase(ArtistRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long id) {
        log.info("Deleting artist with id {}", id);
        repository.deleteById(id);
    }
}
