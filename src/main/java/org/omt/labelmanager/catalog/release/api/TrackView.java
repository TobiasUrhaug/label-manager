package org.omt.labelmanager.catalog.release.api;

import java.util.List;
import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.release.domain.TrackDuration;

public record TrackView(
        Long id,
        List<Artist> artists,
        String name,
        TrackDuration duration,
        Integer position
) {
}
