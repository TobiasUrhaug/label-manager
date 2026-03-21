package org.omt.labelmanager.distribution.distributor.api;

import org.omt.labelmanager.distribution.agreement.domain.PricingAgreement;

public record AgreementView(PricingAgreement agreement, String productionRunDisplayName) {
}
