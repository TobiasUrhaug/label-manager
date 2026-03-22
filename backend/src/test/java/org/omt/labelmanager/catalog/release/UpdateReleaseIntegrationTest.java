package org.omt.labelmanager.catalog.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistEntity;
import org.omt.labelmanager.catalog.artist.infrastructure.ArtistRepository;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseArtistRepository;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class UpdateReleaseIntegrationTest extends AbstractIntegrationTest {

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
    ReleaseCommandApi releaseCommandApi;

    @Test
    @Transactional
    void updateRelease_updatesAllFields() {
        var label = labelTestHelper.createLabel("Label For Update");

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
                1,
                List.of()
        );

        releaseCommandApi.createRelease(
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
                1,
                List.of()
        );
        var newTrack2 = new TrackInput(
                List.of(artist2.getId()),
                "Updated Track 2",
                TrackDuration.parse("5:30"),
                2,
                List.of()
        );

        releaseCommandApi.updateRelease(
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

        var releaseArtists = releaseArtistRepository
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
}
