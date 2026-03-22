package org.omt.labelmanager.distribution.distributor.api;

import org.omt.labelmanager.distribution.agreement.CommissionType;
import org.omt.labelmanager.distribution.agreement.PricingAgreement;

import java.math.RoundingMode;

public record AgreementView(PricingAgreement agreement, String productionRunDisplayName) {

    public String displayCommission() {
        return switch (agreement.commissionType()) {
            case PERCENTAGE -> agreement.commissionValue().stripTrailingZeros().toPlainString() + "%";
            case FIXED_AMOUNT -> agreement.commissionValue().setScale(2, RoundingMode.HALF_UP).toPlainString() + " €";
        };
    }
}
