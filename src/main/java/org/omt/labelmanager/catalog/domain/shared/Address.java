package org.omt.labelmanager.catalog.domain.shared;

import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;

public record Address(
        String street,
        String street2,
        String city,
        String postalCode,
        String country) {

    public static Address fromEmbeddable(AddressEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new Address(
                embeddable.getStreet(),
                embeddable.getStreet2(),
                embeddable.getCity(),
                embeddable.getPostalCode(),
                embeddable.getCountry()
        );
    }
}
