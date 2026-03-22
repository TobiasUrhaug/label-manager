package org.omt.labelmanager.catalog.release.domain;

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
}
