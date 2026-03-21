package org.omt.labelmanager.distribution.distributor.api;

import org.omt.labelmanager.distribution.agreement.domain.CommissionType;
import org.omt.labelmanager.distribution.agreement.domain.PricingAgreement;

public record AgreementView(PricingAgreement agreement, String productionRunDisplayName) {

    public String displayCommission() {
        return switch (agreement.commissionType()) {
            case PERCENTAGE -> agreement.commissionValue().stripTrailingZeros().toPlainString() + "%";
            case FIXED_AMOUNT -> agreement.commissionValue() + " €";
        };
    }
}
