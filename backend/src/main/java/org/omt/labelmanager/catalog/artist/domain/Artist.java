package org.omt.labelmanager.catalog.artist.domain;

import org.omt.labelmanager.catalog.artist.infrastructure.ArtistEntity;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;

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
