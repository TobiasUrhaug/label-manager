package org.omt.labelmanager.catalog.artist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.artist.api.ArtistQueryApi;
import org.omt.labelmanager.catalog.artist.domain.Artist;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistEntity;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class QueryArtistIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArtistQueryApi artistQueryApi;

    @Autowired
    ArtistRepository artistRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void findById_returnsArtist() {
        var entity = new ArtistEntity("Test Artist");
        artistRepository.save(entity);

        var found = artistQueryApi.findById(entity.getId());
        assertThat(found).isPresent();
        assertThat(found.get().artistName()).isEqualTo("Test Artist");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        var found = artistQueryApi.findById(999999L);
        assertThat(found).isEmpty();
    }

    @Test
    void getAllArtists_returnsAllArtists() {
        artistRepository.deleteAll();
        var entity1 = new ArtistEntity("Artist One");
        var entity2 = new ArtistEntity("Artist Two");
        artistRepository.save(entity1);
        artistRepository.save(entity2);

        var artists = artistQueryApi.getAllArtists();
        assertThat(artists).hasSize(2);
        assertThat(artists.stream().map(Artist::artistName).toList())
                .containsExactlyInAnyOrder("Artist One", "Artist Two");
    }

    @Test
    void getArtistsForUser_returnsOnlyUserArtists() {
        artistRepository.deleteAll();
        var user1 = createTestUser("query-test-user1@example.com");
        var user2 = createTestUser("query-test-user2@example.com");

        var entity1 = new ArtistEntity("User 1 Artist");
        entity1.setUserId(user1.getId());
        var entity2 = new ArtistEntity("User 2 Artist");
        entity2.setUserId(user2.getId());
        artistRepository.save(entity1);
        artistRepository.save(entity2);

        var artists = artistQueryApi.getArtistsForUser(user1.getId());
        assertThat(artists).hasSize(1);
        assertThat(artists.getFirst().artistName()).isEqualTo("User 1 Artist");
    }

    private UserEntity createTestUser(String email) {
        return userRepository.save(new UserEntity(email, "password", "Test User"));
    }
}
