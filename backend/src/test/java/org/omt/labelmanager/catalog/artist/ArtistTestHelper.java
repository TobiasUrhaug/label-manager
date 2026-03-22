package org.omt.labelmanager.catalog.artist;

import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistEntity;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.springframework.stereotype.Component;

@Component
public class ArtistTestHelper {

    private final ArtistRepository artistRepository;

    public ArtistTestHelper(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    public Artist createArtist(String artistName) {
        ArtistEntity entity = new ArtistEntity(artistName);
        return Artist.fromEntity(artistRepository.save(entity));
    }

    public Artist createArtist(String artistName, Person realName, String email) {
        ArtistEntity entity = new ArtistEntity(artistName);
        if (realName != null) {
            entity.setRealName(PersonEmbeddable.fromPerson(realName));
        }
        entity.setEmail(email);
        return Artist.fromEntity(artistRepository.save(entity));
    }

    public Artist createArtist(
            String artistName,
            Person realName,
            String email,
            Address address
    ) {
        ArtistEntity entity = new ArtistEntity(artistName);
        if (realName != null) {
            entity.setRealName(PersonEmbeddable.fromPerson(realName));
        }
        entity.setEmail(email);
        if (address != null) {
            entity.setAddress(AddressEmbeddable.fromAddress(address));
        }
        return Artist.fromEntity(artistRepository.save(entity));
    }
}
