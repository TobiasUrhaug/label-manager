package org.omt.labelmanager.track;

public record TrackInput(
        String artist,
        String name,
        Integer durationSeconds,
        Integer position
) {
}
