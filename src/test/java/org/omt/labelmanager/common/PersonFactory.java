package org.omt.labelmanager.common;

public final class PersonFactory {

    private PersonFactory() {
    }

    public static Builder aPerson() {
        return new Builder();
    }

    public static Person createDefault() {
        return aPerson().build();
    }

    public static final class Builder {

        private String name = "Default Person";

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Person build() {
            return new Person(name);
        }
    }
}
