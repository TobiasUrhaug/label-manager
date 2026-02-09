package org.omt.labelmanager.catalog.domain.release;

import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.domain.track.Track;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record Release(
        Long id,
        String name,
        LocalDate releaseDate,
        Long labelId,
        List<Artist> artists,
        List<Track> tracks,
        Set<ReleaseFormat> formats
) {

}
