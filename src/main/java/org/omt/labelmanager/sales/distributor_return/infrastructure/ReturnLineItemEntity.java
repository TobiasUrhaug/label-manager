package org.omt.labelmanager.sales.distributor_return.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;

@Entity
@Table(name = "distributor_return_line_item")
public class ReturnLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private DistributorReturnEntity distributorReturn;

    @Column(name = "release_id", nullable = false)
    private Long releaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ReleaseFormat format;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ReturnLineItemEntity() {
    }

    public ReturnLineItemEntity(Long releaseId, ReleaseFormat format, int quantity) {
        this.releaseId = releaseId;
        this.format = format;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public DistributorReturnEntity getDistributorReturn() {
        return distributorReturn;
    }

    public void setDistributorReturn(DistributorReturnEntity distributorReturn) {
        this.distributorReturn = distributorReturn;
    }

    public Long getReleaseId() {
        return releaseId;
    }

    public ReleaseFormat getFormat() {
        return format;
    }

    public int getQuantity() {
        return quantity;
    }
}
