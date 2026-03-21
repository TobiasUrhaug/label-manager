package org.omt.labelmanager.distribution.agreement.application;

import java.math.BigDecimal;

class AgreementValidator {

    private AgreementValidator() {
    }

    static void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }
    }

    static void validateCommissionPercentage(BigDecimal commissionPercentage) {
        if (commissionPercentage == null
                || commissionPercentage.compareTo(BigDecimal.ZERO) < 0
                || commissionPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Commission percentage must be between 0 and 100");
        }
    }
}
