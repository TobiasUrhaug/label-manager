package org.omt.labelmanager.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.artist.persistence.ArtistEntity;
import org.omt.labelmanager.artist.persistence.ArtistRepository;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.omt.labelmanager.release.persistence.ReleaseRepository;
import org.omt.labelmanager.track.TrackDuration;
import org.omt.labelmanager.track.TrackInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReleaseCRUDIntegrationTest {

    @Autowired
    LabelRepository labelRepository;

    @Autowired
    ReleaseRepository releaseRepository;

    @Autowired
    ArtistRepository artistRepository;

    @Autowired
    ReleaseCRUDHandler releaseCRUDHandler;

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

        var trackInput = new TrackInput(
                List.of(artistId),
                "Test Track",
                TrackDuration.parse("3:30"),
                1
        );

        releaseCRUDHandler.createRelease(
                "My Release",
                LocalDate.of(2026, 1, 15),
                labelId,
                List.of(artistId),
                List.of(trackInput)
        );

        assertThat(releaseRepository.findByName("My Release")).isPresent();
    }

    @Test
    void deleteRelease() {
        var label = new LabelEntity("Label For Release Deletion", null, null);
        labelRepository.save(label);

        var savedArtist = artistRepository.save(new ArtistEntity("Artist For Release"));
        var artistId = savedArtist.getId();

        var trackInput = new TrackInput(
                List.of(artistId),
                "Track To Delete",
                TrackDuration.parse("2:00"),
                1
        );

        releaseCRUDHandler.createRelease(
                "Release To Delete",
                LocalDate.of(2026, 1, 15),
                label.getId(),
                List.of(artistId),
                List.of(trackInput)
        );

        var release = releaseRepository.findByName("Release To Delete");
        assertThat(release).isPresent();

        releaseCRUDHandler.delete(release.get().getId());

        assertThat(releaseRepository.findByName("Release To Delete")).isEmpty();
    }
}
