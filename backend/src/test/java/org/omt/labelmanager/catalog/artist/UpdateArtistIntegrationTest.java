package org.omt.labelmanager.catalog.artist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.artist.api.ArtistCommandApi;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistEntity;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateArtistIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArtistCommandApi artistCommandApi;

    @Autowired
    ArtistRepository artistRepository;

    @Test
    void updateArtist_updatesAllFields() {
        var entity = new ArtistEntity("Original Artist");
        entity.setEmail("old@email.com");
        entity.setRealName(new PersonEmbeddable("Old Name"));
        entity.setAddress(new AddressEmbeddable(
                "Old Street", null, "Oslo", "0123", "Norway"
        ));
        artistRepository.save(entity);

        var newAddress = new Address("New Street", "Apt 2", "Bergen", "5020", "Norway");
        var newRealName = new Person("New Real Name");

        artistCommandApi.updateArtist(
                entity.getId(),
                "Updated Artist",
                newRealName,
                "new@email.com",
                newAddress
        );

        var updated = artistRepository.findById(entity.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getArtistName()).isEqualTo("Updated Artist");
        assertThat(updated.get().getEmail()).isEqualTo("new@email.com");
        assertThat(updated.get().getRealName().getName()).isEqualTo("New Real Name");
        assertThat(updated.get().getAddress().getStreet()).isEqualTo("New Street");
        assertThat(updated.get().getAddress().getCity()).isEqualTo("Bergen");
    }
}
