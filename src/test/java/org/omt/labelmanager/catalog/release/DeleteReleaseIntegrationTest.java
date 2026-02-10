package org.omt.labelmanager.catalog.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistEntity;
import org.omt.labelmanager.catalog.infrastructure.persistence.artist.ArtistRepository;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.api.ReleaseCommandApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.domain.TrackInput;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class DeleteReleaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    LabelTestHelper labelTestHelper;

    @Autowired
    ReleaseRepository releaseRepository;

    @Autowired
    ArtistRepository artistRepository;

    @Autowired
    ReleaseCommandApi releaseCommandApi;

    @Test
    void deleteRelease_removesReleaseFromDatabase() {
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

        releaseCommandApi.createRelease(
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

        releaseCommandApi.delete(release.get().getId());

        assertThat(releaseRepository
                .findByName("Release To Delete")).isEmpty();
    }
}
