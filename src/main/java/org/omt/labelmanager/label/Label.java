package org.omt.labelmanager.label;

import org.omt.labelmanager.label.persistence.LabelEntity;

public record Label(Long id, String name) {

    public static Label fromEntity(LabelEntity entity) {
        return new Label(entity.getId(), entity.getName());
    }
}
