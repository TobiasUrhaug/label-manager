package org.omt.labelmanager.catalog.release;

import java.util.List;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;
import org.omt.labelmanager.catalog.release.infrastructure.TrackEntity;

public class TrackMapper {

    public static Track fromEntity(TrackEntity entity, List<Long> artistIds) {
        return new Track(
                entity.getId(),
                artistIds,
                entity.getName(),
                TrackDuration.ofSeconds(entity.getDurationSeconds()),
                entity.getPosition()
        );
    }
}
