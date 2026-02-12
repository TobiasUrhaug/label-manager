package org.omt.labelmanager.catalog.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.artist.ArtistTestHelper;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRemixerRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class TrackRemixerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelTestHelper labelTestHelper;

    @Autowired
    ArtistTestHelper artistTestHelper;

    @Autowired
    ReleaseCommandApi releaseCommandApi;

    @Autowired
    ReleaseRepository releaseRepository;

    @Autowired
    TrackRepository trackRepository;

    @Autowired
    TrackRemixerRepository trackRemixerRepository;

    @Test
    @Transactional
    void addRemixerToTrack_persistsRelationship() {
        var label = labelTestHelper.createLabel("Test Label");
        var artist = artistTestHelper.createArtist("Main Artist");
        var remixer = artistTestHelper.createArtist("Remixer");

        var trackInput = new TrackInput(
                List.of(artist.id()),
                "Test Track",
                TrackDuration.parse("3:30"),
                1,
                List.of()
        );

        releaseCommandApi.createRelease(
                "Release 1",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artist.id()),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseRepository.findByName("Release 1").orElseThrow();
        var trackId = trackRepository.findByReleaseIdOrderByPosition(
                release.getId()
        ).get(0).getId();

        trackRemixerRepository.addRemixerToTrack(trackId, remixer.id());

        var remixerIds = trackRemixerRepository.findRemixerIdsByTrackId(trackId);
        assertThat(remixerIds).containsExactly(remixer.id());
    }

    @Test
    @Transactional
    void findRemixerIdsByTrackId_returnsEmptyListWhenNoRemixers() {
        var label = labelTestHelper.createLabel("Test Label");
        var artist = artistTestHelper.createArtist("Main Artist");

        var trackInput = new TrackInput(
                List.of(artist.id()),
                "Test Track",
                TrackDuration.parse("3:30"),
                1,
                List.of()
        );

        releaseCommandApi.createRelease(
                "Release 2",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artist.id()),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseRepository.findByName("Release 2").orElseThrow();
        var trackId = trackRepository.findByReleaseIdOrderByPosition(
                release.getId()
        ).get(0).getId();

        var remixerIds = trackRemixerRepository.findRemixerIdsByTrackId(trackId);
        assertThat(remixerIds).isEmpty();
    }

    @Test
    @Transactional
    void addRemixerToTrack_allowsMultipleRemixers() {
        var label = labelTestHelper.createLabel("Test Label");
        var artist = artistTestHelper.createArtist("Main Artist");
        var remixer1 = artistTestHelper.createArtist("Remixer 1");
        var remixer2 = artistTestHelper.createArtist("Remixer 2");

        var trackInput = new TrackInput(
                List.of(artist.id()),
                "Test Track",
                TrackDuration.parse("3:30"),
                1,
                List.of()
        );

        releaseCommandApi.createRelease(
                "Release 3",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artist.id()),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseRepository.findByName("Release 3").orElseThrow();
        var trackId = trackRepository.findByReleaseIdOrderByPosition(
                release.getId()
        ).get(0).getId();

        trackRemixerRepository.addRemixerToTrack(trackId, remixer1.id());
        trackRemixerRepository.addRemixerToTrack(trackId, remixer2.id());

        var remixerIds = trackRemixerRepository.findRemixerIdsByTrackId(trackId);
        assertThat(remixerIds).containsExactlyInAnyOrder(
                remixer1.id(),
                remixer2.id()
        );
    }

    @Test
    @Transactional
    void deleteAllByTrackId_removesAllRemixers() {
        var label = labelTestHelper.createLabel("Test Label");
        var artist = artistTestHelper.createArtist("Main Artist");
        var remixer1 = artistTestHelper.createArtist("Remixer 1");
        var remixer2 = artistTestHelper.createArtist("Remixer 2");

        var trackInput = new TrackInput(
                List.of(artist.id()),
                "Test Track",
                TrackDuration.parse("3:30"),
                1,
                List.of()
        );

        releaseCommandApi.createRelease(
                "Release 4",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artist.id()),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseRepository.findByName("Release 4").orElseThrow();
        var trackId = trackRepository.findByReleaseIdOrderByPosition(
                release.getId()
        ).get(0).getId();

        trackRemixerRepository.addRemixerToTrack(trackId, remixer1.id());
        trackRemixerRepository.addRemixerToTrack(trackId, remixer2.id());

        trackRemixerRepository.deleteAllByTrackId(trackId);

        var remixerIds = trackRemixerRepository.findRemixerIdsByTrackId(trackId);
        assertThat(remixerIds).isEmpty();
    }

    @Test
    @Transactional
    void addRemixerToTrack_allowsMainArtistAsRemixer() {
        var label = labelTestHelper.createLabel("Test Label");
        var artist = artistTestHelper.createArtist("Main Artist");

        var trackInput = new TrackInput(
                List.of(artist.id()),
                "Test Track",
                TrackDuration.parse("3:30"),
                1,
                List.of()
        );

        releaseCommandApi.createRelease(
                "Release 5",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artist.id()),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var release = releaseRepository.findByName("Release 5").orElseThrow();
        var trackId = trackRepository.findByReleaseIdOrderByPosition(
                release.getId()
        ).get(0).getId();

        trackRemixerRepository.addRemixerToTrack(trackId, artist.id());

        var remixerIds = trackRemixerRepository.findRemixerIdsByTrackId(trackId);
        assertThat(remixerIds).containsExactly(artist.id());
    }
}
