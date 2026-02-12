package org.omt.labelmanager.catalog.release;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;

public final class TrackFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private TrackFactory() {
        // utility class
    }

    public static Builder aTrack() {
        return new Builder();
    }

    public static Track createDefault() {
        return aTrack().build();
    }

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private List<Long> artistIds = new ArrayList<>();
        private String name = "Default Track";
        private TrackDuration duration =
                TrackDuration.ofSeconds(180);
        private Integer position = 1;
        private List<Long> remixerIds = new ArrayList<>();

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder artistIds(List<Long> artistIds) {
            this.artistIds = artistIds;
            return this;
        }

        public Builder artistId(Long artistId) {
            this.artistIds = List.of(artistId);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder duration(TrackDuration duration) {
            this.duration = duration;
            return this;
        }

        public Builder durationSeconds(int seconds) {
            this.duration = TrackDuration.ofSeconds(seconds);
            return this;
        }

        public Builder position(Integer position) {
            this.position = position;
            return this;
        }

        public Builder remixerIds(List<Long> remixerIds) {
            this.remixerIds = remixerIds;
            return this;
        }

        public Builder remixerId(Long remixerId) {
            this.remixerIds = List.of(remixerId);
            return this;
        }

        public Track build() {
            return new Track(
                    id, artistIds, name, duration, position, remixerIds
            );
        }
    }
}
