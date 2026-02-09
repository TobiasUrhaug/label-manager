package org.omt.labelmanager.catalog.domain.release;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.domain.label.Label;
import org.omt.labelmanager.catalog.domain.track.Track;

public record Release(
        Long id,
        String name,
        LocalDate releaseDate,
        Label label,
        List<Artist> artists,
        List<Track> tracks,
        Set<ReleaseFormat> formats
) {

}
