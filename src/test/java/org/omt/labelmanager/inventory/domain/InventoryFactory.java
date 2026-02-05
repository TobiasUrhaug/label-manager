package org.omt.labelmanager.inventory.domain;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;

public final class InventoryFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private InventoryFactory() {
    }

    public static Builder anInventory() {
        return new Builder();
    }

    public static Inventory createDefault() {
        return anInventory().build();
    }

    public static final class Builder {

        private Long id = counter.getAndIncrement();
        private Long releaseId = 1L;
        private ReleaseFormat format = ReleaseFormat.VINYL;
        private String description = "Original pressing";
        private String manufacturer = "Record Industry";
        private LocalDate manufacturingDate = LocalDate.of(2025, 1, 1);
        private int quantity = 500;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder releaseId(Long releaseId) {
            this.releaseId = releaseId;
            return this;
        }

        public Builder format(ReleaseFormat format) {
            this.format = format;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder manufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
            return this;
        }

        public Builder manufacturingDate(LocalDate manufacturingDate) {
            this.manufacturingDate = manufacturingDate;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Inventory build() {
            return new Inventory(
                    id,
                    releaseId,
                    format,
                    description,
                    manufacturer,
                    manufacturingDate,
                    quantity
            );
        }
    }
}
