package org.omt.labelmanager.catalog.artist.api;

import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;

public interface ArtistCommandApi {

    void createArtist(
            String artistName,
            Person realName,
            String email,
            Address address,
            Long userId
    );

    void updateArtist(
            Long id,
            String artistName,
            Person realName,
            String email,
            Address address
    );

    void delete(Long id);
}
