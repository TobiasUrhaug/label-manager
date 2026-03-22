package org.omt.labelmanager.catalog.domain.shared;

import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;

public record Person(String name) {

    public static Person fromEmbeddable(PersonEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new Person(embeddable.getName());
    }
}
