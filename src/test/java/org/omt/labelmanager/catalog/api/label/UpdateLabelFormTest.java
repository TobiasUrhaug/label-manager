package org.omt.labelmanager.catalog.api.label;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UpdateLabelFormTest {

    @Test
    void toAddress_returnsAddress_whenStreetIsProvided() {
        var form = new UpdateLabelForm();
        form.setStreet("123 Main St");
        form.setStreet2("Apt 4B");
        form.setCity("Oslo");
        form.setPostalCode("0123");
        form.setCountry("Norway");

        var address = form.toAddress();

        assertThat(address).isNotNull();
        assertThat(address.street()).isEqualTo("123 Main St");
        assertThat(address.street2()).isEqualTo("Apt 4B");
        assertThat(address.city()).isEqualTo("Oslo");
        assertThat(address.postalCode()).isEqualTo("0123");
        assertThat(address.country()).isEqualTo("Norway");
    }

    @Test
    void toAddress_returnsNull_whenStreetIsBlank() {
        var form = new UpdateLabelForm();
        form.setStreet("");

        assertThat(form.toAddress()).isNull();
    }

    @Test
    void toAddress_returnsNull_whenStreetIsNull() {
        var form = new UpdateLabelForm();

        assertThat(form.toAddress()).isNull();
    }

    @Test
    void toOwner_returnsPerson_whenOwnerNameIsProvided() {
        var form = new UpdateLabelForm();
        form.setOwnerName("John Doe");

        var owner = form.toOwner();

        assertThat(owner).isNotNull();
        assertThat(owner.name()).isEqualTo("John Doe");
    }

    @Test
    void toOwner_returnsNull_whenOwnerNameIsBlank() {
        var form = new UpdateLabelForm();
        form.setOwnerName("");

        assertThat(form.toOwner()).isNull();
    }

    @Test
    void toOwner_returnsNull_whenOwnerNameIsNull() {
        var form = new UpdateLabelForm();

        assertThat(form.toOwner()).isNull();
    }
}
