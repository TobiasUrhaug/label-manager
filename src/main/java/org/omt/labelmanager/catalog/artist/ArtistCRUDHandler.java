package org.omt.labelmanager.catalog.artist;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.artist.persistence.ArtistEntity;
import org.omt.labelmanager.catalog.artist.persistence.ArtistRepository;
import org.omt.labelmanager.catalog.shared.Address;
import org.omt.labelmanager.catalog.shared.Person;
import org.omt.labelmanager.catalog.shared.persistence.AddressEmbeddable;
import org.omt.labelmanager.catalog.shared.persistence.PersonEmbeddable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ArtistCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(ArtistCRUDHandler.class);

    private final ArtistRepository repository;

    public ArtistCRUDHandler(ArtistRepository repository) {
        this.repository = repository;
    }

    public List<Artist> getAllArtists() {
        List<Artist> artists = repository.findAll().stream().map(Artist::fromEntity).toList();
        log.debug("Retrieved {} artists", artists.size());
        return artists;
    }

    public List<Artist> getArtistsForUser(Long userId) {
        List<Artist> artists = repository.findByUserId(userId).stream()
                .map(Artist::fromEntity)
                .toList();
        log.debug("Retrieved {} artists for user {}", artists.size(), userId);
        return artists;
    }

    public Optional<Artist> findById(Long id) {
        Optional<Artist> artist = repository.findById(id).map(Artist::fromEntity);
        if (artist.isEmpty()) {
            log.debug("Artist with id {} not found", id);
        }
        return artist;
    }

    public void createArtist(
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

    @Transactional
    public void updateArtist(
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

    @Transactional
    public void delete(Long id) {
        log.info("Deleting artist with id {}", id);
        repository.deleteById(id);
    }
}
