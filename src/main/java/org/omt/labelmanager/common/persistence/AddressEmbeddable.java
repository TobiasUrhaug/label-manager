package org.omt.labelmanager.common.persistence;

import jakarta.persistence.Embeddable;
import org.omt.labelmanager.common.Address;

@Embeddable
public class AddressEmbeddable {

    private String street;
    private String street2;
    private String city;
    private String postalCode;
    private String country;

    protected AddressEmbeddable() {
    }

    public AddressEmbeddable(
            String street,
            String street2,
            String city,
            String postalCode,
            String country) {
        this.street = street;
        this.street2 = street2;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public static AddressEmbeddable fromAddress(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressEmbeddable(
                address.street(),
                address.street2(),
                address.city(),
                address.postalCode(),
                address.country()
        );
    }

    public String getStreet() {
        return street;
    }

    public String getStreet2() {
        return street2;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
