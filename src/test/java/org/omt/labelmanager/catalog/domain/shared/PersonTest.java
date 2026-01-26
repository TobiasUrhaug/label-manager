package org.omt.labelmanager.catalog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.shared.persistence.PersonEmbeddable;

class PersonTest {

    @Test
    void fromEmbeddable_mapsAllFields() {
        var embeddable = new PersonEmbeddable("John Doe");

        var person = Person.fromEmbeddable(embeddable);

        assertThat(person.name()).isEqualTo("John Doe");
    }

    @Test
    void fromEmbeddable_returnsNull_whenEmbeddableIsNull() {
        assertThat(Person.fromEmbeddable(null)).isNull();
    }

    @Test
    void embeddableFromPerson_mapsAllFields() {
        var person = new Person("Jane Doe");

        var embeddable = PersonEmbeddable.fromPerson(person);

        assertThat(embeddable.getName()).isEqualTo("Jane Doe");
    }

    @Test
    void embeddableFromPerson_returnsNull_whenPersonIsNull() {
        assertThat(PersonEmbeddable.fromPerson(null)).isNull();
    }
}
