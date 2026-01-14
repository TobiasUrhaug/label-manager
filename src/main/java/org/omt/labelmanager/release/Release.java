package org.omt.labelmanager.release;

import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.release.persistence.ReleaseEntity;

import java.time.LocalDate;

public record Release(
        Long id,
        String name,
        LocalDate releaseDate,
        Label label
) {

    public static Release fromEntity(ReleaseEntity entity) {
        return new Release(
                entity.getId(),
                entity.getName(),
                entity.getReleaseDate(),
                Label.fromEntity(entity.getLabel())
        );
    }

}
