package org.omt.labelmanager.distribution.agreement.api;

public class AgreementNotFoundException extends RuntimeException {

    public AgreementNotFoundException(Long agreementId) {
        super("Pricing agreement not found: " + agreementId);
    }
}
