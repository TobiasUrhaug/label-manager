package org.omt.labelmanager.catalog.artist;

import java.util.concurrent.atomic.AtomicLong;
import org.omt.labelmanager.common.Address;
import org.omt.labelmanager.common.Person;

public final class ArtistFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private ArtistFactory() {
        // utility class
    }

    public static Builder anArtist() {
        return new Builder();
    }

    public static Artist createDefault() {
        return anArtist().build();
    }

    // ----------------------------------------------------------------------

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private String artistName = "Default Artist";
        private Person realName = null;
        private String email = null;
        private Address address = null;
        private Long userId = 1L;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder artistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder realName(Person realName) {
            this.realName = realName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder address(Address address) {
            this.address = address;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Artist build() {
            return new Artist(id, artistName, realName, email, address, userId);
        }
    }
}
