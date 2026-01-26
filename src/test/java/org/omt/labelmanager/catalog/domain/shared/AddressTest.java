package org.omt.labelmanager.catalog.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.omt.labelmanager.catalog.shared.persistence.AddressEmbeddable;

class AddressTest {

    @Test
    void fromEmbeddable_mapsAllFields() {
        var embeddable = new AddressEmbeddable(
                "123 Main St",
                "Apt 4B",
                "Oslo",
                "0123",
                "Norway"
        );

        var address = Address.fromEmbeddable(embeddable);

        assertThat(address.street()).isEqualTo("123 Main St");
        assertThat(address.street2()).isEqualTo("Apt 4B");
        assertThat(address.city()).isEqualTo("Oslo");
        assertThat(address.postalCode()).isEqualTo("0123");
        assertThat(address.country()).isEqualTo("Norway");
    }

    @Test
    void fromEmbeddable_returnsNull_whenEmbeddableIsNull() {
        assertThat(Address.fromEmbeddable(null)).isNull();
    }

    @Test
    void embeddableFromAddress_mapsAllFields() {
        var address = new Address(
                "456 Oak Ave",
                "Suite 100",
                "Bergen",
                "5020",
                "Norway"
        );

        var embeddable = AddressEmbeddable.fromAddress(address);

        assertThat(embeddable.getStreet()).isEqualTo("456 Oak Ave");
        assertThat(embeddable.getStreet2()).isEqualTo("Suite 100");
        assertThat(embeddable.getCity()).isEqualTo("Bergen");
        assertThat(embeddable.getPostalCode()).isEqualTo("5020");
        assertThat(embeddable.getCountry()).isEqualTo("Norway");
    }

    @Test
    void embeddableFromAddress_returnsNull_whenAddressIsNull() {
        assertThat(AddressEmbeddable.fromAddress(null)).isNull();
    }
}
