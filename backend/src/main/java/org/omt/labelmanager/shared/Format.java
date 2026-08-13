package org.omt.labelmanager.shared;

public enum Format {
    DIGITAL(false),
    VINYL(true),
    CASSETTE(true),
    CD(true);

    private final boolean physical;

    Format(boolean physical) {
        this.physical = physical;
    }

    public boolean isPhysical() {
        return physical;
    }
}
