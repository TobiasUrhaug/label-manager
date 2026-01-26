package org.omt.labelmanager.catalog.domain.track;

import java.util.List;
import org.omt.labelmanager.catalog.domain.artist.Artist;
import org.omt.labelmanager.catalog.infrastructure.persistence.track.TrackEntity;

public record Track(
        Long id,
        List<Artist> artists,
        String name,
        TrackDuration duration,
        Integer position
) {

    public static Track fromEntity(TrackEntity entity) {
        List<Artist> artists = entity.getArtists().stream()
                .map(Artist::fromEntity)
                .toList();
        return new Track(
                entity.getId(),
                artists,
                entity.getName(),
                TrackDuration.ofSeconds(entity.getDurationSeconds()),
                entity.getPosition()
        );
    }

}
