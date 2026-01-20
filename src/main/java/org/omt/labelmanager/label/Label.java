package org.omt.labelmanager.label;

import org.omt.labelmanager.label.persistence.LabelEntity;

public record Label(Long id, String name, String email, String website) {

    public static Label fromEntity(LabelEntity entity) {
        return new Label(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getWebsite()
        );
    }
}
