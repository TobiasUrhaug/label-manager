package org.omt.labelmanager.catalog.api.label;

import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;

public class CreateLabelForm {

    private String labelName;
    private String email;
    private String website;
    private String ownerName;
    private String street;
    private String street2;
    private String city;
    private String postalCode;
    private String country;

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Address toAddress() {
        if (street == null || street.isBlank()) {
            return null;
        }
        return new Address(street, street2, city, postalCode, country);
    }

    public Person toOwner() {
        if (ownerName == null || ownerName.isBlank()) {
            return null;
        }
        return new Person(ownerName);
    }
}
