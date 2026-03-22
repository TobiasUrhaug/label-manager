package org.omt.labelmanager.catalog.artist.application;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class ArtistQueryApiImpl implements ArtistQueryApi {

    private static final Logger log = LoggerFactory.getLogger(ArtistQueryApiImpl.class);

    private final ArtistRepository repository;

    ArtistQueryApiImpl(ArtistRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Artist> findById(Long id) {
        Optional<Artist> artist = repository.findById(id).map(Artist::fromEntity);
        if (artist.isEmpty()) {
            log.debug("Artist with id {} not found", id);
        }
        return artist;
    }

    @Override
    public List<Artist> getAllArtists() {
        List<Artist> artists = repository.findAll().stream().map(Artist::fromEntity).toList();
        log.debug("Retrieved {} artists", artists.size());
        return artists;
    }

    @Override
    public List<Artist> getArtistsForUser(Long userId) {
        List<Artist> artists = repository.findByUserId(userId).stream()
                .map(Artist::fromEntity)
                .toList();
        log.debug("Retrieved {} artists for user {}", artists.size(), userId);
        return artists;
    }
}
