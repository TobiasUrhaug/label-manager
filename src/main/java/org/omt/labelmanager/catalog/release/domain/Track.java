package org.omt.labelmanager.catalog.release.domain;

import java.util.List;

public record Track(
        Long id,
        List<Long> artistIds,
        String name,
        TrackDuration duration,
        Integer position
) {
}
