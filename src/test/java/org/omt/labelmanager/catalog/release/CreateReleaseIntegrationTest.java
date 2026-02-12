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
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class CreateReleaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelTestHelper labelTestHelper;

    @Autowired
    ReleaseRepository releaseRepository;

    @Autowired
    ArtistRepository artistRepository;

    @Autowired
    ReleaseCommandApi releaseCommandApi;

    @Test
    void createRelease_persistsReleaseWithAllFields() {
        var savedLabel = labelTestHelper.createLabel("The Label");
        var labelId = savedLabel.id();
        var savedArtist = artistRepository.save(
                new ArtistEntity("Test Artist")
        );
        var artistId = savedArtist.getId();

        var trackInput = new TrackInput(
                List.of(artistId),
                "Test Track",
                TrackDuration.parse("3:30"),
                1,
                List.of()
        );

        releaseCommandApi.createRelease(
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
}
