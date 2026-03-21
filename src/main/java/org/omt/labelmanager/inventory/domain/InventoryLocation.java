package org.omt.labelmanager.inventory.domain;

/**
 * Identifies one endpoint of an inventory movement.
 *
 * <p>Combines a {@link LocationType} with an optional location ID
 * (required only for {@link LocationType#DISTRIBUTOR}).
 * Factory methods enforce these constraints and make call sites
 * self-documenting.
 */
public record InventoryLocation(LocationType type, Long id) {

    public static InventoryLocation warehouse() {
        return new InventoryLocation(LocationType.WAREHOUSE, null);
    }

    public static InventoryLocation distributor(Long distributorId) {
        return new InventoryLocation(
                LocationType.DISTRIBUTOR, distributorId
        );
    }

    public static InventoryLocation external() {
        return new InventoryLocation(LocationType.EXTERNAL, null);
    }

    public static InventoryLocation bandcamp() {
        return new InventoryLocation(LocationType.BANDCAMP, null);
    }
}
