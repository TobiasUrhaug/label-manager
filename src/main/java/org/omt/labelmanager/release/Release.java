package org.omt.labelmanager.release;

import java.time.LocalDate;
import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.release.persistence.ReleaseEntity;

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
