package org.omt.labelmanager.finance.domain.cost;

public record CostOwner(CostOwnerType type, Long id) {

    public static CostOwner release(Long id) {
        return new CostOwner(CostOwnerType.RELEASE, id);
    }

    public static CostOwner label(Long id) {
        return new CostOwner(CostOwnerType.LABEL, id);
    }

    public static CostOwner user(Long id) {
        return new CostOwner(CostOwnerType.USER, id);
    }
}
