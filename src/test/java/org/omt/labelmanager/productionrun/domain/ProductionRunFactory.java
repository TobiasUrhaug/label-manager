package org.omt.labelmanager.productionrun.domain;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;

public final class ProductionRunFactory {

    private static final AtomicLong counter = new AtomicLong(1);

    private ProductionRunFactory() {
    }

    public static Builder aProductionRun() {
        return new Builder();
    }

    public static ProductionRun createDefault() {
        return aProductionRun().build();
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

        public ProductionRun build() {
            return new ProductionRun(
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
