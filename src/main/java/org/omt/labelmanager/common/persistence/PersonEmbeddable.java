package org.omt.labelmanager.common.persistence;

import jakarta.persistence.Embeddable;
import org.omt.labelmanager.common.Person;

@Embeddable
public class PersonEmbeddable {

    private String name;

    protected PersonEmbeddable() {
    }

    public PersonEmbeddable(String name) {
        this.name = name;
    }

    public static PersonEmbeddable fromPerson(Person person) {
        if (person == null) {
            return null;
        }
        return new PersonEmbeddable(person.name());
    }

    public String getName() {
        return name;
    }
}
