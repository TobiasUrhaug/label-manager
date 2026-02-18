package org.omt.labelmanager.inventory.domain;

/**
 * Identifies the type of a physical inventory location in the movement model.
 *
 * <p>Every {@code InventoryMovement} records a transfer from one location to another.
 * The combination of {@code LocationType} and an optional location ID fully identifies
 * each endpoint of the transfer:
 *
 * <ul>
 *   <li>{@link #WAREHOUSE} — the label's own stock. There is only one warehouse; no
 *       additional ID is needed.</li>
 *   <li>{@link #DISTRIBUTOR} — an external distributor holding inventory on behalf of
 *       the label. Must be paired with the distributor's {@code Long} ID.</li>
 *   <li>{@link #EXTERNAL} — outside the label's system entirely (i.e. sold to end
 *       customers). No additional ID is needed.</li>
 * </ul>
 *
 * <p>Standard movement patterns:
 * <pre>
 *   Allocation : WAREHOUSE       → DISTRIBUTOR(distributorId)
 *   Sale       : DISTRIBUTOR(id) → EXTERNAL
 *   Return     : DISTRIBUTOR(id) → WAREHOUSE
 * </pre>
 */
public enum LocationType {

    /** The label's own warehouse stock. */
    WAREHOUSE,

    /**
     * An external distributor holding inventory on behalf of the label.
     * Must be accompanied by a non-null distributor ID.
     */
    DISTRIBUTOR,

    /**
     * Inventory that has left the label's system entirely — i.e. sold to end customers.
     * No accompanying ID is required.
     */
    EXTERNAL
}
