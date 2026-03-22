package org.omt.labelmanager.catalog.artist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.artist.api.ArtistCommandApi;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class CreateArtistIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArtistCommandApi artistCommandApi;

    @Autowired
    ArtistRepository artistRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void createArtist_persistsAllFields() {
        var user = createTestUser("create-artist@example.com");
        var address = new Address("123 Music Lane", null, "Oslo", "0123", "Norway");
        var realName = new Person("John Smith");

        artistCommandApi.createArtist("DJ Cool", realName, "dj@cool.com", address, user.getId());

        var savedArtist = artistRepository.findByArtistName("DJ Cool");
        assertThat(savedArtist).isPresent();
        assertThat(savedArtist.get().getEmail()).isEqualTo("dj@cool.com");
        assertThat(savedArtist.get().getRealName().getName()).isEqualTo("John Smith");
        assertThat(savedArtist.get().getAddress().getStreet()).isEqualTo("123 Music Lane");
        assertThat(savedArtist.get().getAddress().getCity()).isEqualTo("Oslo");
    }

    @Test
    void createArtist_withOnlyRequiredFields() {
        artistCommandApi.createArtist("Minimal Artist", null, null, null, null);

        var savedArtist = artistRepository.findByArtistName("Minimal Artist");
        assertThat(savedArtist).isPresent();
        assertThat(savedArtist.get().getEmail()).isNull();
        assertThat(savedArtist.get().getRealName()).isNull();
        assertThat(savedArtist.get().getAddress()).isNull();
    }

    private UserEntity createTestUser(String email) {
        return userRepository.save(new UserEntity(email, "password", "Test User"));
    }
}
