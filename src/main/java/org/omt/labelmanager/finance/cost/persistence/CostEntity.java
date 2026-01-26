package org.omt.labelmanager.finance.cost.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.omt.labelmanager.finance.domain.cost.CostType;

@Entity
@Table(name = "cost")
public class CostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String currency;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(name = "vat_amount", nullable = false)
    private BigDecimal vatAmount;

    @Column(name = "vat_rate", nullable = false)
    private BigDecimal vatRate;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false)
    private CostType costType;

    @Column(name = "incurred_on", nullable = false)
    private LocalDate incurredOn;

    private String description;

    @Embedded
    private CostOwnerEmbeddable owner;

    @Column(name = "document_reference")
    private String documentReference;

    protected CostEntity() {
    }

    public CostEntity(
            String currency,
            BigDecimal netAmount,
            BigDecimal vatAmount,
            BigDecimal vatRate,
            BigDecimal grossAmount,
            CostType costType,
            LocalDate incurredOn,
            String description,
            CostOwnerEmbeddable owner,
            String documentReference
    ) {
        this.currency = currency;
        this.netAmount = netAmount;
        this.vatAmount = vatAmount;
        this.vatRate = vatRate;
        this.grossAmount = grossAmount;
        this.costType = costType;
        this.incurredOn = incurredOn;
        this.description = description;
        this.owner = owner;
        this.documentReference = documentReference;
    }

    public Long getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public CostType getCostType() {
        return costType;
    }

    public LocalDate getIncurredOn() {
        return incurredOn;
    }

    public String getDescription() {
        return description;
    }

    public CostOwnerEmbeddable getOwner() {
        return owner;
    }

    public String getDocumentReference() {
        return documentReference;
    }
}
