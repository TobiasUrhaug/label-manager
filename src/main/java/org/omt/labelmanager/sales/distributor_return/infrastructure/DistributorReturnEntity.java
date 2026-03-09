package org.omt.labelmanager.sales.distributor_return.infrastructure;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "distributor_return")
public class DistributorReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "label_id", nullable = false)
    private Long labelId;

    @Column(name = "distributor_id", nullable = false)
    private Long distributorId;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "notes")
    private String notes;

    @OneToMany(
            mappedBy = "distributorReturn",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReturnLineItemEntity> lineItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected DistributorReturnEntity() {
    }

    public DistributorReturnEntity(
            Long labelId,
            Long distributorId,
            LocalDate returnDate,
            String notes
    ) {
        this.labelId = labelId;
        this.distributorId = distributorId;
        this.returnDate = returnDate;
        this.notes = notes;
    }

    public void addLineItem(ReturnLineItemEntity item) {
        lineItems.add(item);
        item.setDistributorReturn(this);
    }

    /**
     * Removes all line items from this return. Used when editing a return to replace
     * the existing line items with a new set. Orphan removal on the OneToMany
     * relationship ensures the cleared items are deleted from the database.
     */
    public void clearLineItems() {
        lineItems.clear();
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public String getNotes() {
        return notes;
    }

    public List<ReturnLineItemEntity> getLineItems() {
        return lineItems;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
