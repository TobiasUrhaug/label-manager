package org.omt.labelmanager.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InventoryLocationTest {

    @Test
    void warehouse_createsLocationWithWarehouseTypeAndNullId() {
        var location = InventoryLocation.warehouse();

        assertThat(location.type()).isEqualTo(LocationType.WAREHOUSE);
        assertThat(location.id()).isNull();
    }

    @Test
    void distributor_createsLocationWithDistributorTypeAndGivenId() {
        var location = InventoryLocation.distributor(42L);

        assertThat(location.type()).isEqualTo(LocationType.DISTRIBUTOR);
        assertThat(location.id()).isEqualTo(42L);
    }

    @Test
    void external_createsLocationWithExternalTypeAndNullId() {
        var location = InventoryLocation.external();

        assertThat(location.type()).isEqualTo(LocationType.EXTERNAL);
        assertThat(location.id()).isNull();
    }

    @Test
    void bandcamp_createsLocationWithBandcampTypeAndNullId() {
        var location = InventoryLocation.bandcamp();

        assertThat(location.type()).isEqualTo(LocationType.BANDCAMP);
        assertThat(location.id()).isNull();
    }
}
