package org.omt.labelmanager.catalog.release;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ReleaseFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private ReleaseFactory() {
        // utility class
    }

    public static Builder aRelease() {
        return new Builder();
    }

    public static Release createDefault() {
        return aRelease().build();
    }

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private String name = "Default Release";
        private LocalDate releaseDate = LocalDate.now();
        private Long labelId = 1L;
        private List<Long> artistIds = new ArrayList<>();
        private List<Track> tracks = new ArrayList<>(
                List.of(TrackFactory.createDefault())
        );
        private Set<ReleaseFormat> formats =
                new HashSet<>(Set.of(ReleaseFormat.DIGITAL));

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder releaseDate(LocalDate releaseDate) {
            this.releaseDate = releaseDate;
            return this;
        }

        public Builder labelId(Long labelId) {
            this.labelId = labelId;
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

        public Builder tracks(List<Track> tracks) {
            this.tracks = tracks;
            return this;
        }

        public Builder tracks(Track... tracks) {
            this.tracks = List.of(tracks);
            return this;
        }

        public Builder formats(Set<ReleaseFormat> formats) {
            this.formats = formats;
            return this;
        }

        public Builder formats(ReleaseFormat... formats) {
            this.formats = Set.of(formats);
            return this;
        }

        public Release build() {
            return new Release(
                    id,
                    name,
                    releaseDate,
                    labelId,
                    artistIds,
                    tracks,
                    formats
            );
        }
    }
}
