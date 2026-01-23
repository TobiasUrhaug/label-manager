package org.omt.labelmanager.label;

import java.util.concurrent.atomic.AtomicLong;
import org.omt.labelmanager.common.Address;
import org.omt.labelmanager.common.Person;

public final class LabelFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private LabelFactory() {
        // utility class
    }

    public static Builder aLabel() {
        return new Builder();
    }

    public static Label createDefault() {
        return aLabel().build();
    }

    // ----------------------------------------------------------------------

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private String name = "Default Label";
        private String email = "default@example.com";
        private String website = "https://example.com";
        private Address address = null;
        private Person owner = null;
        private Long userId = 1L;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder website(String website) {
            this.website = website;
            return this;
        }

        public Builder address(Address address) {
            this.address = address;
            return this;
        }

        public Builder owner(Person owner) {
            this.owner = owner;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Label build() {
            return new Label(id, name, email, website, address, owner, userId);
        }
    }
}
