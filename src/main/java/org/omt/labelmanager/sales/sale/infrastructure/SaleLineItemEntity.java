package org.omt.labelmanager.sales.sale.infrastructure;

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

import java.math.BigDecimal;

@Entity
@Table(name = "sale_line_item")
public class SaleLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private SaleEntity sale;

    @Column(name = "release_id", nullable = false)
    private Long releaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ReleaseFormat format;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;

    @Column(name = "currency", nullable = false)
    private String currency = "EUR";

    protected SaleLineItemEntity() {
    }

    public SaleLineItemEntity(
            Long releaseId,
            ReleaseFormat format,
            int quantity,
            BigDecimal unitPrice,
            String currency
    ) {
        this.releaseId = releaseId;
        this.format = format;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public SaleEntity getSale() {
        return sale;
    }

    public void setSale(SaleEntity sale) {
        this.sale = sale;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public String getCurrency() {
        return currency;
    }
}
