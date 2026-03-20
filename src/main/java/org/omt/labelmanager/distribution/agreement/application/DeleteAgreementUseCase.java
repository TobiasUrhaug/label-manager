package org.omt.labelmanager.distribution.agreement.application;

import org.omt.labelmanager.distribution.agreement.api.AgreementNotFoundException;
import org.omt.labelmanager.distribution.agreement.infrastructure.PricingAgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteAgreementUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteAgreementUseCase.class);

    private final PricingAgreementRepository repository;

    DeleteAgreementUseCase(PricingAgreementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(Long agreementId) {
        if (!repository.existsById(agreementId)) {
            throw new AgreementNotFoundException(agreementId);
        }

        repository.deleteById(agreementId);
        log.info("Deleted pricing agreement {}", agreementId);
    }
}
