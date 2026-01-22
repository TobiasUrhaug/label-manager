package org.omt.labelmanager.track;

public record TrackInput(
        String artist,
        String name,
        TrackDuration duration,
        Integer position
) {
}
