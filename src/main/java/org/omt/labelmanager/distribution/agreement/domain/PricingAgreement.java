package org.omt.labelmanager.distribution.agreement.domain;

import org.omt.labelmanager.distribution.agreement.infrastructure.PricingAgreementEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record PricingAgreement(
        Long id,
        Long distributorId,
        Long productionRunId,
        BigDecimal unitPrice,
        BigDecimal commissionPercentage,
        Instant createdAt
) {

    public static PricingAgreement fromEntity(PricingAgreementEntity entity) {
        return new PricingAgreement(
                entity.getId(),
                entity.getDistributorId(),
                entity.getProductionRunId(),
                entity.getUnitPrice(),
                entity.getCommissionPercentage(),
                entity.getCreatedAt()
        );
    }
}
