package org.omt.labelmanager.distribution.agreement.api;

public class DuplicateAgreementException extends RuntimeException {

    public DuplicateAgreementException(Long distributorId, Long productionRunId) {
        super("A pricing agreement already exists for distributor " + distributorId
                + " and production run " + productionRunId);
    }
}
