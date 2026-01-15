package org.omt.labelmanager.label;

import java.util.concurrent.atomic.AtomicLong;

public final class LabelFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private LabelFactory() {
        // utility class
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Label createDefault() {
        return builder().build();
    }

    // ----------------------------------------------------------------------

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private String name = "Default Label";

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

        public Label build() {
            return new Label(id, name);
        }
    }
}
