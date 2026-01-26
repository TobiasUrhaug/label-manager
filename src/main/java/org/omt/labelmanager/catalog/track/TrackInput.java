package org.omt.labelmanager.catalog.track;

import java.util.List;

public record TrackInput(
        List<Long> artistIds,
        String name,
        TrackDuration duration,
        Integer position
) {
}
