package org.omt.labelmanager.catalog.release;

public record TrackDuration(int totalSeconds) {

    public static TrackDuration parse(String mmss) {
        if (mmss == null || mmss.isBlank()) {
            return new TrackDuration(0);
        }
        String[] parts = mmss.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Duration must be in MM:SS format"
            );
        }
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        return new TrackDuration(minutes * 60 + seconds);
    }

    public static TrackDuration ofSeconds(int seconds) {
        return new TrackDuration(seconds);
    }

    public String formatted() {
        return String.format(
                "%d:%02d", totalSeconds / 60, totalSeconds % 60
        );
    }

}
