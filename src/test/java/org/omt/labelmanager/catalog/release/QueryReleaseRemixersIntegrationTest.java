package org.omt.labelmanager.catalog.release;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;
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
import org.omt.labelmanager.catalog.release.infrastructure.TrackRemixerRepository;
import org.omt.labelmanager.catalog.release.infrastructure.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class QueryReleaseRemixersIntegrationTest
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

    @Autowired
    TrackRepository trackRepository;

    @Autowired
    TrackRemixerRepository trackRemixerRepository;

    @Test
    @Transactional
    void findById_loadsRemixersForTracks() {
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
                "Query Release 1",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artist.id()),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var releaseEntity = releaseRepository
                .findByName("Query Release 1")
                .orElseThrow();
        var trackId = trackRepository.findByReleaseIdOrderByPosition(
                releaseEntity.getId()
        ).get(0).getId();

        trackRemixerRepository.addRemixerToTrack(trackId, remixer1.id());
        trackRemixerRepository.addRemixerToTrack(trackId, remixer2.id());

        var release = releaseQueryApi.findById(
                releaseEntity.getId()
        ).orElseThrow();

        assertThat(release.tracks()).hasSize(1);
        assertThat(release.tracks().get(0).remixerIds())
                .containsExactlyInAnyOrder(remixer1.id(), remixer2.id());
    }

    @Test
    @Transactional
    void findById_returnsEmptyRemixersWhenNoneExist() {
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
                "Query Release 2",
                LocalDate.of(2026, 1, 15),
                label.id(),
                List.of(artist.id()),
                List.of(trackInput),
                Set.of(ReleaseFormat.DIGITAL)
        );

        var releaseEntity = releaseRepository
                .findByName("Query Release 2")
                .orElseThrow();

        var release = releaseQueryApi.findById(
                releaseEntity.getId()
        ).orElseThrow();

        assertThat(release.tracks()).hasSize(1);
        assertThat(release.tracks().get(0).remixerIds()).isEmpty();
    }
}
