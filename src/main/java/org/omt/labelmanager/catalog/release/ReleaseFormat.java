package org.omt.labelmanager.catalog.release;

public enum ReleaseFormat {

    DIGITAL(false),
    VINYL(true),
    CASSETTE(true),
    CD(true);

    private final boolean physical;

    ReleaseFormat(boolean physical) {
        this.physical = physical;
    }

    public boolean isPhysical() {
        return physical;
    }
}
