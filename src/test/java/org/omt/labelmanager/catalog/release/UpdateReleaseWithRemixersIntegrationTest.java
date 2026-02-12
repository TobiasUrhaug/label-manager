package org.omt.labelmanager.catalog.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.artist.ArtistTestHelper;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateReleaseWithRemixersIntegrationTest
        extends AbstractIntegrationTest {

    @Autowired
    LabelTestHelper labelTestHelper;

    @Autowired
    ArtistTestHelper artistTestHelper;

    @Autowired
    ReleaseCommandApi releaseCommandApi;

    @Autowired
    ReleaseQueryApi releaseQueryApi;

    @Autowired
    ReleaseRepository releaseRepository;

    @Test
    void updateRelease_updatesRemixersForTracks() {
        var label = labelTestHelper.createLabel("Test Label");
        var artist = artistTestHelper.createArtist("Main Artist");
        var remixer1 = artistTestHelper.createArtist("Remixer 1");
        var remixer2 = artistTestHelper.createArtist("Remixer 2");

        var originalTrack = new TrackInput(
                List.of(artist.id()),
                "Original Track",
                TrackDuration.parse("3:00"),
                1,
                List.of(remixer1.id())
        );

        releaseCommandApi.createRelease(
                "Test Release",
                LocalDate.of(2026, 1, 1),
                label.id(),
                List.of(artist.id()),
                List.of(originalTrack),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var releaseEntity = releaseRepository
                .findByName("Test Release")
                .orElseThrow();

        var updatedTrack = new TrackInput(
                List.of(artist.id()),
                "Updated Track",
                TrackDuration.parse("4:00"),
                1,
                List.of(remixer2.id())
        );

        releaseCommandApi.updateRelease(
                releaseEntity.getId(),
                "Test Release",
                LocalDate.of(2026, 1, 1),
                List.of(artist.id()),
                List.of(updatedTrack),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseQueryApi.findById(
                releaseEntity.getId()
        ).orElseThrow();

        assertThat(release.tracks()).hasSize(1);
        assertThat(release.tracks().get(0).name())
                .isEqualTo("Updated Track");
        assertThat(release.tracks().get(0).remixerIds())
                .containsExactly(remixer2.id());
    }

    @Test
    void updateRelease_removesRemixersWhenNotProvided() {
        var label = labelTestHelper.createLabel("Test Label");
        var artist = artistTestHelper.createArtist("Main Artist");
        var remixer = artistTestHelper.createArtist("Remixer");

        var originalTrack = new TrackInput(
                List.of(artist.id()),
                "Original Track",
                TrackDuration.parse("3:00"),
                1,
                List.of(remixer.id())
        );

        releaseCommandApi.createRelease(
                "Another Release",
                LocalDate.of(2026, 1, 1),
                label.id(),
                List.of(artist.id()),
                List.of(originalTrack),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var releaseEntity = releaseRepository
                .findByName("Another Release")
                .orElseThrow();

        var updatedTrack = new TrackInput(
                List.of(artist.id()),
                "Updated Track",
                TrackDuration.parse("3:00"),
                1,
                List.of()
        );

        releaseCommandApi.updateRelease(
                releaseEntity.getId(),
                "Another Release",
                LocalDate.of(2026, 1, 1),
                List.of(artist.id()),
                List.of(updatedTrack),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseQueryApi.findById(
                releaseEntity.getId()
        ).orElseThrow();

        assertThat(release.tracks()).hasSize(1);
        assertThat(release.tracks().get(0).remixerIds()).isEmpty();
    }
}
