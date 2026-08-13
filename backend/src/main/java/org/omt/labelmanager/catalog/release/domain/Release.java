package org.omt.labelmanager.catalog.release.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.shared.Format;

public record Release(
        Long id,
        String name,
        LocalDate releaseDate,
        Long labelId,
        List<Long> artistIds,
        List<Track> tracks,
        Set<Format> formats) {}
