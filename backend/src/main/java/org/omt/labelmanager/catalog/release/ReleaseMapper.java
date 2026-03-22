package org.omt.labelmanager.catalog.release;

import java.util.List;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.catalog.release.domain.Track;
import org.omt.labelmanager.catalog.release.infrastructure.ReleaseEntity;

public class ReleaseMapper {

    public static Release fromEntity(
            ReleaseEntity entity,
            List<Long> artistIds,
            List<Track> tracks
    ) {
        return new Release(
                entity.getId(),
                entity.getName(),
                entity.getReleaseDate(),
                entity.getLabelId(),
                artistIds,
                tracks,
                entity.getFormats()
        );
    }
}
