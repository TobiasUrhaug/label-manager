package org.omt.labelmanager.catalog.artist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.application.ArtistCRUDHandler;
import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistRepository;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ArtistCRUDIntegrationTest {

    @Autowired
    ArtistRepository repo;

    @Autowired
    ArtistCRUDHandler artistCRUDHandler;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void createArtist_persistsAllFields() {
        var address = new Address("123 Music Lane", null, "Oslo", "0123", "Norway");
        var realName = new Person("John Smith");

        artistCRUDHandler.createArtist("DJ Cool", realName, "dj@cool.com", address, null);

        var savedArtist = repo.findByArtistName("DJ Cool");
        assertThat(savedArtist).isPresent();
        assertThat(savedArtist.get().getEmail()).isEqualTo("dj@cool.com");
        assertThat(savedArtist.get().getRealName().getName()).isEqualTo("John Smith");
        assertThat(savedArtist.get().getAddress().getStreet()).isEqualTo("123 Music Lane");
        assertThat(savedArtist.get().getAddress().getCity()).isEqualTo("Oslo");
    }

    @Test
    void createArtist_withOnlyRequiredFields() {
        artistCRUDHandler.createArtist("Minimal Artist", null, null, null, null);

        var savedArtist = repo.findByArtistName("Minimal Artist");
        assertThat(savedArtist).isPresent();
        assertThat(savedArtist.get().getEmail()).isNull();
        assertThat(savedArtist.get().getRealName()).isNull();
        assertThat(savedArtist.get().getAddress()).isNull();
    }

    @Test
    void findById_returnsArtist() {
        var entity = new ArtistEntity("Test Artist");
        repo.save(entity);

        var found = artistCRUDHandler.findById(entity.getId());
        assertThat(found).isPresent();
        assertThat(found.get().artistName()).isEqualTo("Test Artist");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        var found = artistCRUDHandler.findById(999999L);
        assertThat(found).isEmpty();
    }

    @Test
    void getAllArtists_returnsAllArtists() {
        repo.deleteAll();
        var entity1 = new ArtistEntity("Artist One");
        var entity2 = new ArtistEntity("Artist Two");
        repo.save(entity1);
        repo.save(entity2);

        var artists = artistCRUDHandler.getAllArtists();
        assertThat(artists).hasSize(2);
        assertThat(artists.stream().map(Artist::artistName).toList())
                .containsExactlyInAnyOrder("Artist One", "Artist Two");
    }

    @Test
    void updateArtist_updatesAllFields() {
        var entity = new ArtistEntity("Original Artist");
        entity.setEmail("old@email.com");
        entity.setRealName(new PersonEmbeddable("Old Name"));
        entity.setAddress(new AddressEmbeddable("Old Street", null, "Oslo", "0123", "Norway"));
        repo.save(entity);

        var newAddress = new Address("New Street", "Apt 2", "Bergen", "5020", "Norway");
        var newRealName = new Person("New Real Name");

        artistCRUDHandler.updateArtist(
                entity.getId(),
                "Updated Artist",
                newRealName,
                "new@email.com",
                newAddress
        );

        var updated = repo.findById(entity.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getArtistName()).isEqualTo("Updated Artist");
        assertThat(updated.get().getEmail()).isEqualTo("new@email.com");
        assertThat(updated.get().getRealName().getName()).isEqualTo("New Real Name");
        assertThat(updated.get().getAddress().getStreet()).isEqualTo("New Street");
        assertThat(updated.get().getAddress().getCity()).isEqualTo("Bergen");
    }

    @Test
    void deleteArtist_removesFromDatabase() {
        var entity = new ArtistEntity("Artist To Delete");
        repo.save(entity);

        assertThat(repo.findById(entity.getId())).isPresent();

        artistCRUDHandler.delete(entity.getId());

        assertThat(repo.findById(entity.getId())).isEmpty();
    }

    @Test
    void artistWithAddress_persistsAndRetrievesAddress() {
        var address = new AddressEmbeddable(
                "456 Studio Road",
                "Unit B",
                "Trondheim",
                "7030",
                "Norway"
        );
        var artist = new ArtistEntity("Artist With Address");
        artist.setAddress(address);
        repo.save(artist);

        var retrieved = repo.findByArtistName("Artist With Address");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAddress()).isNotNull();
        assertThat(retrieved.get().getAddress().getStreet()).isEqualTo("456 Studio Road");
        assertThat(retrieved.get().getAddress().getStreet2()).isEqualTo("Unit B");
        assertThat(retrieved.get().getAddress().getCity()).isEqualTo("Trondheim");
        assertThat(retrieved.get().getAddress().getCountry()).isEqualTo("Norway");
    }

    @Test
    void artistWithRealName_persistsAndRetrievesRealName() {
        var realName = new PersonEmbeddable("Jane Doe");
        var artist = new ArtistEntity("Stage Name");
        artist.setRealName(realName);
        repo.save(artist);

        var retrieved = repo.findByArtistName("Stage Name");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRealName()).isNotNull();
        assertThat(retrieved.get().getRealName().getName()).isEqualTo("Jane Doe");
    }
}
