package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;

import java.time.LocalDate;

public class AddProductionRunForm {

    private ReleaseFormat format;
    private String description;
    private String manufacturer;
    private LocalDate manufacturingDate;
    private int quantity;

    public ReleaseFormat getFormat() {
        return format;
    }

    public void setFormat(ReleaseFormat format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public void setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
