package org.omt.labelmanager.label;

import java.util.concurrent.atomic.AtomicLong;

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

        public Label build() {
            return new Label(id, name, email, website);
        }
    }
}
