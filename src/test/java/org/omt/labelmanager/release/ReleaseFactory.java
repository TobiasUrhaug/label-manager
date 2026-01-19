package org.omt.labelmanager.release;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.omt.labelmanager.label.Label;
import org.omt.labelmanager.label.LabelFactory;

public final class ReleaseFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private ReleaseFactory() {
        // utility class
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Release createDefault() {
        return builder().build();
    }

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private String name = "Default Release";
        private LocalDate releaseDate = LocalDate.now();
        private Label label = LabelFactory.createDefault();

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

        public Builder releaseDate(LocalDate releaseDate) {
            this.releaseDate = releaseDate;
            return this;
        }

        public Builder label(Label label) {
            this.label = label;
            return this;
        }

        public Release build() {
            return new Release(id, name, releaseDate, label);
        }
    }
}
