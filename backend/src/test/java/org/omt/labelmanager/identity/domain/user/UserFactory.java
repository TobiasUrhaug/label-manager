package org.omt.labelmanager.identity.domain.user;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public final class UserFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private UserFactory() {
        // utility class
    }

    public static Builder anUser() {
        return new Builder();
    }

    public static User createDefault() {
        return anUser().build();
    }

    // ----------------------------------------------------------------------

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private String email = "user@example.com";
        private String password = "hashedPassword123";
        private String displayName = "Default User";
        private Instant createdAt = Instant.now();

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public User build() {
            return new User(id, email, password, displayName, createdAt);
        }
    }
}
