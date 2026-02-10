package org.omt.labelmanager.catalog.release;

import java.util.List;
import org.omt.labelmanager.catalog.release.persistence.TrackEntity;

public record Track(
        Long id,
        List<Long> artistIds,
        String name,
        TrackDuration duration,
        Integer position
) {

    static Track fromEntity(TrackEntity entity, List<Long> artistIds) {
        return new Track(
                entity.getId(),
                artistIds,
                entity.getName(),
                TrackDuration.ofSeconds(entity.getDurationSeconds()),
                entity.getPosition()
        );
    }
}
