package org.omt.labelmanager.catalog.domain.track;

import java.util.List;
import org.omt.labelmanager.catalog.domain.artist.Artist;

public record Track(
        Long id,
        List<Artist> artists,
        String name,
        TrackDuration duration,
        Integer position
) {

}
