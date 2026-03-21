package org.omt.labelmanager.distribution.agreement.domain;

import org.omt.labelmanager.distribution.agreement.infrastructure.PricingAgreementEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record PricingAgreement(
        Long id,
        Long distributorId,
        Long productionRunId,
        BigDecimal unitPrice,
        CommissionType commissionType,
        BigDecimal commissionValue,
        Instant createdAt
) {

    public PricingAgreement {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }
        if (commissionType == CommissionType.FIXED_AMOUNT) {
            if (commissionValue == null || commissionValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Commission value must be greater than zero");
            }
        } else {
            if (commissionValue == null || commissionValue.compareTo(BigDecimal.ZERO) < 0
                    || commissionValue.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Commission percentage must be between 0 and 100");
            }
        }
    }

    public static PricingAgreement fromEntity(PricingAgreementEntity entity) {
        return new PricingAgreement(
                entity.getId(),
                entity.getDistributorId(),
                entity.getProductionRunId(),
                entity.getUnitPrice(),
                entity.getCommissionType(),
                entity.getCommissionValue(),
                entity.getCreatedAt()
        );
    }
}
