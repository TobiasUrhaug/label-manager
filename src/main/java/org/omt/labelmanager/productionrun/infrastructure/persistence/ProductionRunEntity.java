package org.omt.labelmanager.productionrun.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;

@Entity
@Table(name = "production_run")
public class ProductionRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_id", nullable = false)
    private Long releaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReleaseFormat format;

    private String description;

    @Column(nullable = false)
    private String manufacturer;

    @Column(name = "manufacturing_date", nullable = false)
    private LocalDate manufacturingDate;

    @Column(nullable = false)
    private int quantity;

    protected ProductionRunEntity() {
    }

    public ProductionRunEntity(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {
        this.releaseId = releaseId;
        this.format = format;
        this.description = description;
        this.manufacturer = manufacturer;
        this.manufacturingDate = manufacturingDate;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public ReleaseFormat getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public int getQuantity() {
        return quantity;
    }
}
