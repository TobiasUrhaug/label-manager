package org.omt.labelmanager.catalog.artist.api;

import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;

public class CreateArtistForm {

    private String artistName;
    private String realName;
    private String email;
    private String street;
    private String street2;
    private String city;
    private String postalCode;
    private String country;

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public Person toRealName() {
        if (realName == null || realName.isBlank()) {
            return null;
        }
        return new Person(realName);
    }
}
