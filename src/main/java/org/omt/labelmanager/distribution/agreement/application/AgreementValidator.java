package org.omt.labelmanager.distribution.agreement.application;

import org.omt.labelmanager.distribution.agreement.domain.CommissionType;

import java.math.BigDecimal;

class AgreementValidator {

    private AgreementValidator() {
    }

    static void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }
    }

    static void validateCommissionValue(CommissionType type, BigDecimal value) {
        if (type == CommissionType.FIXED_AMOUNT) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Commission value must be greater than zero");
            }
        } else {
            if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Commission value must be greater than zero");
            }
            if (value.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Commission percentage must be between 0 and 100");
            }
        }
    }
}
