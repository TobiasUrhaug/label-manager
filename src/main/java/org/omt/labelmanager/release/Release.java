package org.omt.labelmanager.release;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.release.persistence.ReleaseEntity;
import org.omt.labelmanager.track.Track;

public record Release(
        Long id,
        String name,
        LocalDate releaseDate,
        Label label,
        List<Track> tracks
) {

    public static Release fromEntity(ReleaseEntity entity) {
        List<Track> tracks = entity.getTracks().stream()
                .map(Track::fromEntity)
                .toList();
        return new Release(
                entity.getId(),
                entity.getName(),
                entity.getReleaseDate(),
                Label.fromEntity(entity.getLabel()),
                tracks
        );
    }

}
