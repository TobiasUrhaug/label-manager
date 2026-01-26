package org.omt.labelmanager.catalog.artist;

import org.omt.labelmanager.catalog.artist.persistence.ArtistEntity;
import org.omt.labelmanager.catalog.shared.Address;
import org.omt.labelmanager.catalog.shared.Person;

public record Artist(
        Long id,
        String artistName,
        Person realName,
        String email,
        Address address,
        Long userId) {

    public static Artist fromEntity(ArtistEntity entity) {
        return new Artist(
                entity.getId(),
                entity.getArtistName(),
                Person.fromEmbeddable(entity.getRealName()),
                entity.getEmail(),
                Address.fromEmbeddable(entity.getAddress()),
                entity.getUserId()
        );
    }
}
