package org.omt.labelmanager.catalog.release.domain;

import java.util.List;

public record TrackInput(
        List<Long> artistIds,
        String name,
        TrackDuration duration,
        Integer position,
        List<Long> remixerIds
) {
}
