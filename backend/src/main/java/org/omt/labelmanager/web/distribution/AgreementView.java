package org.omt.labelmanager.web.distribution;

import java.math.RoundingMode;
import org.omt.labelmanager.distribution.agreement.api.PricingAgreement;

public record AgreementView(PricingAgreement agreement, String productionRunDisplayName) {

    public String displayCommission() {
        return switch (agreement.commissionType()) {
            case PERCENTAGE ->
                    agreement.commissionValue().stripTrailingZeros().toPlainString() + "%";
            case FIXED_AMOUNT ->
                    agreement.commissionValue().setScale(2, RoundingMode.HALF_UP).toPlainString()
                            + " €";
        };
    }
}
