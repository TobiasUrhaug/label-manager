package org.omt.labelmanager.catalog.api.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.domain.track.TrackDuration;

class UpdateReleaseFormTest {

    @Test
    void toTrackInputs_convertsTracksWithPositions() {
        var form = new UpdateReleaseForm();

        var track1 = new UpdateReleaseForm.TrackForm();
        track1.setArtistIds(List.of(1L, 2L));
        track1.setName("First Track");
        track1.setDuration("3:30");

        var track2 = new UpdateReleaseForm.TrackForm();
        track2.setArtistIds(List.of(2L));
        track2.setName("Second Track");
        track2.setDuration("4:15");

        form.setTracks(List.of(track1, track2));

        var trackInputs = form.toTrackInputs();

        assertThat(trackInputs).hasSize(2);

        assertThat(trackInputs.get(0).artistIds()).containsExactly(1L, 2L);
        assertThat(trackInputs.get(0).name()).isEqualTo("First Track");
        assertThat(trackInputs.get(0).duration()).isEqualTo(TrackDuration.parse("3:30"));
        assertThat(trackInputs.get(0).position()).isEqualTo(1);

        assertThat(trackInputs.get(1).artistIds()).containsExactly(2L);
        assertThat(trackInputs.get(1).name()).isEqualTo("Second Track");
        assertThat(trackInputs.get(1).duration()).isEqualTo(TrackDuration.parse("4:15"));
        assertThat(trackInputs.get(1).position()).isEqualTo(2);
    }
}
