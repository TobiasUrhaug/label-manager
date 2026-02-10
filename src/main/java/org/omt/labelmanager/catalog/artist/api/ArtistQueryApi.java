package org.omt.labelmanager.catalog.artist.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.artist.domain.Artist;

public interface ArtistQueryApi {

    Optional<Artist> findById(Long id);

    List<Artist> getAllArtists();

    List<Artist> getArtistsForUser(Long userId);
}
