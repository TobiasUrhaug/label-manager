package org.omt.labelmanager.catalog.shared;

import org.omt.labelmanager.catalog.shared.persistence.PersonEmbeddable;

public record Person(String name) {

    public static Person fromEmbeddable(PersonEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new Person(embeddable.getName());
    }
}
