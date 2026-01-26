package org.omt.labelmanager.finance.cost.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.omt.labelmanager.finance.domain.cost.CostOwner;
import org.omt.labelmanager.finance.domain.cost.CostOwnerType;

@Embeddable
public class CostOwnerEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type")
    private CostOwnerType ownerType;

    @Column(name = "owner_id")
    private Long ownerId;

    protected CostOwnerEmbeddable() {
    }

    public CostOwnerEmbeddable(CostOwnerType ownerType, Long ownerId) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
    }

    public static CostOwnerEmbeddable fromCostOwner(CostOwner owner) {
        if (owner == null) {
            return null;
        }
        return new CostOwnerEmbeddable(owner.type(), owner.id());
    }

    public CostOwner toCostOwner() {
        return new CostOwner(ownerType, ownerId);
    }

    public CostOwnerType getOwnerType() {
        return ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }
}
