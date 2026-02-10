package org.omt.labelmanager.catalog.release;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record Release(
        Long id,
        String name,
        LocalDate releaseDate,
        Long labelId,
        List<Long> artistIds,
        List<Track> tracks,
        Set<ReleaseFormat> formats
) {

    static Release fromEntity(
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
