package org.omt.labelmanager.catalog.label;

import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;

public record Label(
        Long id,
        String name,
        String email,
        String website,
        Address address,
        Person owner,
        Long userId
) {

    static Label fromEntity(LabelEntity entity) {
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
