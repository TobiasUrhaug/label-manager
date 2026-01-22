package org.omt.labelmanager.track;

import org.omt.labelmanager.track.persistence.TrackEntity;

public record Track(
        Long id,
        String artist,
        String name,
        Integer durationSeconds,
        Integer position
) {

    public static Track fromEntity(TrackEntity entity) {
        return new Track(
                entity.getId(),
                entity.getArtist(),
                entity.getName(),
                entity.getDurationSeconds(),
                entity.getPosition()
        );
    }

}
