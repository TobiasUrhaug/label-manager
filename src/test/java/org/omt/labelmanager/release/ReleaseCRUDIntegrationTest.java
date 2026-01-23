package org.omt.labelmanager.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.artist.persistence.ArtistEntity;
import org.omt.labelmanager.artist.persistence.ArtistRepository;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.omt.labelmanager.release.persistence.ReleaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class ReleaseCRUDIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private RestTestClient restClient;

    @Autowired
    LabelRepository labelRepository;

    @Autowired
    ReleaseRepository releaseRepository;

    @Autowired
    ArtistRepository artistRepository;

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
    void createRelease() {
        var savedLabel = labelRepository.save(new LabelEntity("The Label", null, null));
        var labelId = savedLabel.getId();
        var savedArtist = artistRepository.save(new ArtistEntity("Test Artist"));
        var artistId = savedArtist.getId();

        restClient
                .post()
                .uri("/labels/" + labelId + "/releases")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("releaseName=My+Release&releaseDate=2026-01-15"
                        + "&artistIds=" + artistId
                        + "&tracks[0].artistIds=" + artistId
                        + "&tracks[0].name=Test+Track"
                        + "&tracks[0].duration=3:30")
                .exchange()
                .expectStatus()
                .is3xxRedirection();

        assertThat(releaseRepository.findByName("My Release")).isPresent();
    }

    @Test
    void deleteRelease() {
        var label = new LabelEntity("WronglyNamedLabel", null, null);
        labelRepository.save(label);

        assertThat(labelRepository
                .findAll()
                .stream()
                .map(LabelEntity::getName)
                .toList()
        )
                .contains("WronglyNamedLabel");

        restClient
                .delete()
                .uri("/labels/" + label.getId())
                .exchange()
                .expectStatus().is3xxRedirection();

        assertThat(labelRepository
                .findAll()
                .stream()
                .map(LabelEntity::getName)
                .toList()
        )
                .doesNotContain("WronglyNamedLabel");
    }
}
