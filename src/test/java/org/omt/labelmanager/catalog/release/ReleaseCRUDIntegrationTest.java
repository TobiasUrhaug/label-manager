package org.omt.labelmanager.catalog.release;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistRepository;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandFacade;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryFacade;
import org.omt.labelmanager.catalog.release.persistence.ReleaseArtistRepository;
import org.omt.labelmanager.catalog.release.persistence.ReleaseRepository;
import org.omt.labelmanager.catalog.release.persistence.TrackRepository;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ReleaseCRUDIntegrationTest {

    @Autowired
    LabelTestHelper labelTestHelper;

    @Autowired
    ReleaseRepository releaseRepository;

    @Autowired
    ArtistRepository artistRepository;

    @Autowired
    TrackRepository trackRepository;

    @Autowired
    ReleaseArtistRepository releaseArtistRepository;

    @Autowired
    ReleaseCommandFacade releaseCommandFacade;

    @Autowired
    ReleaseQueryFacade releaseQueryFacade;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url", postgres::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );
        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Test
    void createRelease() {
        var savedLabel =
                labelTestHelper.createLabel("The Label");
        var labelId = savedLabel.id();
        var savedArtist = artistRepository.save(
                new ArtistEntity("Test Artist")
        );
        var artistId = savedArtist.getId();

        var trackInput = new TrackInput(
                List.of(artistId),
                "Test Track",
                TrackDuration.parse("3:30"),
                1
        );

        releaseCommandFacade.createRelease(
                "My Release",
                LocalDate.of(2026, 1, 15),
                labelId,
                List.of(artistId),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        assertThat(releaseRepository.findByName("My Release"))
                .isPresent();
    }

    @Test
    @Transactional
    void updateRelease() {
        var label =
                labelTestHelper.createLabel("Label For Update");

        var artist1 = artistRepository.save(
                new ArtistEntity("Artist One")
        );
        var artist2 = artistRepository.save(
                new ArtistEntity("Artist Two")
        );

        var originalTrack = new TrackInput(
                List.of(artist1.getId()),
                "Original Track",
                TrackDuration.parse("3:00"),
                1
        );

        releaseCommandFacade.createRelease(
                "Original Release",
                LocalDate.of(2026, 1, 1),
                label.id(),
                List.of(artist1.getId()),
                List.of(originalTrack),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseRepository
                .findByName("Original Release").orElseThrow();
        var releaseId = release.getId();

        var newTrack1 = new TrackInput(
                List.of(artist1.getId(), artist2.getId()),
                "Updated Track 1",
                TrackDuration.parse("4:00"),
                1
        );
        var newTrack2 = new TrackInput(
                List.of(artist2.getId()),
                "Updated Track 2",
                TrackDuration.parse("5:30"),
                2
        );

        releaseCommandFacade.updateRelease(
                releaseId,
                "Updated Release",
                LocalDate.of(2026, 6, 15),
                List.of(artist1.getId(), artist2.getId()),
                List.of(newTrack1, newTrack2),
                Set.of(ReleaseFormat.VINYL, ReleaseFormat.CD)
        );

        var updatedRelease = releaseRepository
                .findById(releaseId).orElseThrow();
        assertThat(updatedRelease.getName())
                .isEqualTo("Updated Release");
        assertThat(updatedRelease.getReleaseDate())
                .isEqualTo(LocalDate.of(2026, 6, 15));

        var releaseArtists =
                releaseArtistRepository
                        .findArtistIdsByReleaseId(releaseId);
        assertThat(releaseArtists).hasSize(2);

        var tracks = trackRepository
                .findByReleaseIdOrderByPosition(releaseId);
        assertThat(tracks).hasSize(2);
        assertThat(tracks.get(0).getName())
                .isEqualTo("Updated Track 1");
        assertThat(tracks.get(1).getName())
                .isEqualTo("Updated Track 2");
    }

    @Test
    void deleteRelease() {
        var label = labelTestHelper.createLabel(
                "Label For Release Deletion"
        );

        var savedArtist = artistRepository.save(
                new ArtistEntity("Artist For Release")
        );
        var artistId = savedArtist.getId();

        var trackInput = new TrackInput(
                List.of(artistId),
                "Track To Delete",
                TrackDuration.parse("2:00"),
                1
        );

        releaseCommandFacade.createRelease(
                "Release To Delete",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artistId),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseRepository
                .findByName("Release To Delete");
        assertThat(release).isPresent();

        releaseCommandFacade.delete(release.get().getId());

        assertThat(releaseRepository
                .findByName("Release To Delete")).isEmpty();
    }
}
