package org.omt.labelmanager.inventory;

/**
 * Identifies the type of a physical inventory location in the movement model.
 *
 * <p>Every {@code InventoryMovement} records a transfer from one location to another. The
 * combination of {@code LocationType} and an optional location ID fully identifies each endpoint of
 * the transfer:
 *
 * <ul>
 *   <li>{@link #WAREHOUSE} — the label's own stock. There is only one warehouse; no additional ID
 *       is needed.
 *   <li>{@link #DISTRIBUTOR} — an external distributor holding inventory on behalf of the label.
 *       Must be paired with the distributor's {@code Long} ID.
 *   <li>{@link #EXTERNAL} — outside the label's system entirely: both where manufactured units come
 *       from and where sold units go. No additional ID is needed.
 * </ul>
 *
 * <p>Standard movement patterns:
 *
 * <pre>
 *   Production          : EXTERNAL        → WAREHOUSE
 *   Allocation          : WAREHOUSE       → DISTRIBUTOR(distributorId)
 *   Sale                : DISTRIBUTOR(id) → EXTERNAL
 *   Return              : DISTRIBUTOR(id) → WAREHOUSE
 *   Bandcamp reservation: WAREHOUSE       → BANDCAMP
 *   Bandcamp cancellation: BANDCAMP       → WAREHOUSE
 * </pre>
 */
public enum LocationType {

    /** The label's own warehouse stock. */
    WAREHOUSE,

    /**
     * An external distributor holding inventory on behalf of the label. Must be accompanied by a
     * non-null distributor ID.
     */
    DISTRIBUTOR,

    /**
     * Outside the label's system entirely — units sold to end customers, and the pressing plant
     * manufactured units arrive from. No accompanying ID is required.
     */
    EXTERNAL,

    /**
     * Bandcamp platform holding inventory reserved for online sales. No accompanying ID is
     * required.
     */
    BANDCAMP
}
