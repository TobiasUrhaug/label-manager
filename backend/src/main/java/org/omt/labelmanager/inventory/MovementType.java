package org.omt.labelmanager.inventory;

/**
 * The business event a movement records.
 *
 * <p>{@code PRODUCTION} is manufacture entering the warehouse. Recording it as a movement is what
 * makes every location balance uniformly {@code Σ in − Σ out}, with no caller adding the run's
 * manufactured quantity back in.
 */
public enum MovementType {
    PRODUCTION,
    ALLOCATION,
    SALE,
    RETURN
}
