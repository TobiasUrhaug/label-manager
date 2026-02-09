package org.omt.labelmanager.catalog.label.api;

import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.Label;

public interface LabelCommandFacade {

    Label createLabel(
            String labelName,
            String email,
            String website,
            Address address,
            Person owner,
            Long userId
    );

    void delete(Long id);

    void updateLabel(
            Long id,
            String name,
            String email,
            String website,
            Address address,
            Person owner
    );

}
