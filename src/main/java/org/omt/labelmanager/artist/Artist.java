package org.omt.labelmanager.artist;

import org.omt.labelmanager.artist.persistence.ArtistEntity;
import org.omt.labelmanager.common.Address;
import org.omt.labelmanager.common.Person;

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
