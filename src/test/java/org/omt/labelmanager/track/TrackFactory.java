package org.omt.labelmanager.track;

import java.util.concurrent.atomic.AtomicLong;

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
        private String artist = "Default Artist";
        private String name = "Default Track";
        private TrackDuration duration = TrackDuration.ofSeconds(180);
        private Integer position = 1;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder artist(String artist) {
            this.artist = artist;
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

        public Track build() {
            return new Track(id, artist, name, duration, position);
        }
    }
}
