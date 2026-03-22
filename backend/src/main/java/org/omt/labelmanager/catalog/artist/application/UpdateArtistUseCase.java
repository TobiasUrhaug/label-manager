package org.omt.labelmanager.catalog.artist.application;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class UpdateArtistUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateArtistUseCase.class);

    private final ArtistRepository repository;

    UpdateArtistUseCase(ArtistRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(
            Long id,
            String artistName,
            Person realName,
            String email,
            Address address
    ) {
        log.info("Updating artist {}", id);
        repository.findById(id).ifPresent(entity -> {
            entity.setArtistName(artistName);
            if (realName != null) {
                entity.setRealName(PersonEmbeddable.fromPerson(realName));
            }
            entity.setEmail(email);
            if (address != null) {
                entity.setAddress(AddressEmbeddable.fromAddress(address));
            }
            repository.save(entity);
        });
    }
}
