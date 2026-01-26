package org.omt.labelmanager.catalog.shared;

public final class AddressFactory {

    private AddressFactory() {
    }

    public static Builder anAddress() {
        return new Builder();
    }

    public static Address createDefault() {
        return anAddress().build();
    }

    public static final class Builder {

        private String street = "123 Main Street";
        private String street2 = null;
        private String city = "Oslo";
        private String postalCode = "0123";
        private String country = "Norway";

        private Builder() {
        }

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder street2(String street2) {
            this.street2 = street2;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Address build() {
            return new Address(street, street2, city, postalCode, country);
        }
    }
}
