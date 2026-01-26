package org.omt.labelmanager.catalog.domain.label;

import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.persistence.LabelEntity;

public record Label(Long id, String name, String email, String website, Address address,
                    Person owner, Long userId) {

    public static Label fromEntity(LabelEntity entity) {
        return new Label(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getWebsite(),
                Address.fromEmbeddable(entity.getAddress()),
                Person.fromEmbeddable(entity.getOwner()),
                entity.getUserId()
        );
    }
}
