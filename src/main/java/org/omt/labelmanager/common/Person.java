package org.omt.labelmanager.common;

import org.omt.labelmanager.common.persistence.PersonEmbeddable;

public record Person(String name) {

    public static Person fromEmbeddable(PersonEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new Person(embeddable.getName());
    }
}
