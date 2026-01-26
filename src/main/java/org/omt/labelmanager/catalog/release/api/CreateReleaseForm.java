package org.omt.labelmanager.catalog.release.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.catalog.domain.track.TrackDuration;
import org.omt.labelmanager.catalog.domain.track.TrackInput;

public class CreateReleaseForm {

    private String releaseName;
    private String releaseDate;
    private List<Long> artistIds = new ArrayList<>();
    private List<TrackForm> tracks = new ArrayList<>();
    private List<String> formats = new ArrayList<>();

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public List<Long> getArtistIds() {
        return artistIds;
    }

    public void setArtistIds(List<Long> artistIds) {
        this.artistIds = artistIds;
    }

    public List<TrackForm> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackForm> tracks) {
        this.tracks = tracks;
    }

    public List<String> getFormats() {
        return formats;
    }

    public void setFormats(List<String> formats) {
        this.formats = formats;
    }

    public Set<ReleaseFormat> toReleaseFormats() {
        return formats.stream()
                .map(ReleaseFormat::valueOf)
                .collect(Collectors.toSet());
    }

    public List<TrackInput> toTrackInputs() {
        return IntStream.range(0, tracks.size())
                .mapToObj(i -> {
                    TrackForm track = tracks.get(i);
                    return new TrackInput(
                            track.getArtistIds(),
                            track.getName(),
                            TrackDuration.parse(track.getDuration()),
                            i + 1
                    );
                })
                .toList();
    }

    public static class TrackForm {
        private List<Long> artistIds = new ArrayList<>();
        private String name;
        private String duration;

        public List<Long> getArtistIds() {
            return artistIds;
        }

        public void setArtistIds(List<Long> artistIds) {
            this.artistIds = artistIds;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }
    }
}
