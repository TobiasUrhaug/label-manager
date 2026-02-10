package org.omt.labelmanager.catalog.artist.application;

import org.omt.labelmanager.catalog.artist.infrastructure.ArtistEntity;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class CreateArtistUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateArtistUseCase.class);

    private final ArtistRepository repository;

    CreateArtistUseCase(ArtistRepository repository) {
        this.repository = repository;
    }

    public void execute(
            String artistName,
            Person realName,
            String email,
            Address address,
            Long userId
    ) {
        log.info("Creating artist '{}' for user {}", artistName, userId);
        var entity = new ArtistEntity(artistName);
        entity.setUserId(userId);
        if (realName != null) {
            entity.setRealName(PersonEmbeddable.fromPerson(realName));
        }
        entity.setEmail(email);
        if (address != null) {
            entity.setAddress(AddressEmbeddable.fromAddress(address));
        }
        repository.save(entity);
    }
}
