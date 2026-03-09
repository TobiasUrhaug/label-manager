package org.omt.labelmanager.sales.sale.infrastructure;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale")
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label_id", nullable = false)
    private Long labelId;

    @Column(name = "distributor_id", nullable = false)
    private Long distributorId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private ChannelType channel;

    @Column(name = "notes")
    private String notes;

    @OneToMany(
            mappedBy = "sale",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SaleLineItemEntity> lineItems = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency = "EUR";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected SaleEntity() {
    }

    public SaleEntity(
            Long labelId,
            Long distributorId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            String currency
    ) {
        this.labelId = labelId;
        this.distributorId = distributorId;
        this.saleDate = saleDate;
        this.channel = channel;
        this.notes = notes;
        this.currency = currency;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addLineItem(SaleLineItemEntity item) {
        lineItems.add(item);
        item.setSale(this);
        recalculateTotal();
    }

    /**
     * Removes all line items from this sale. Used when editing a sale to replace
     * the existing line items with a new set. Orphan removal on the OneToMany
     * relationship ensures the cleared items are deleted from the database.
     */
    public void clearLineItems() {
        lineItems.clear();
        this.totalAmount = BigDecimal.ZERO;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    private void recalculateTotal() {
        this.totalAmount = lineItems.stream()
                .map(SaleLineItemEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Long getLabelId() {
        return labelId;
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public String getNotes() {
        return notes;
    }

    public List<SaleLineItemEntity> getLineItems() {
        return lineItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
