package org.omt.labelmanager.release.api;

import java.util.ArrayList;
import java.util.List;
import org.omt.labelmanager.track.TrackDuration;
import org.omt.labelmanager.track.TrackInput;

public class CreateReleaseForm {

    private String releaseName;
    private String releaseDate;
    private List<TrackForm> tracks = new ArrayList<>();

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

    public List<TrackForm> getTracks() {
        return tracks;
    }

    public void setTracks(List<TrackForm> tracks) {
        this.tracks = tracks;
    }

    public List<TrackInput> toTrackInputs() {
        List<TrackInput> inputs = new ArrayList<>();
        for (int i = 0; i < tracks.size(); i++) {
            TrackForm track = tracks.get(i);
            inputs.add(new TrackInput(
                    track.getArtist(),
                    track.getName(),
                    TrackDuration.parse(track.getDuration()),
                    i + 1
            ));
        }
        return inputs;
    }

    public static class TrackForm {
        private String artist;
        private String name;
        private String duration;

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
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
