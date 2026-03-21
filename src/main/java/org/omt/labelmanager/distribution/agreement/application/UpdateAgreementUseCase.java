package org.omt.labelmanager.distribution.agreement.application;

import org.omt.labelmanager.distribution.agreement.api.AgreementNotFoundException;
import org.omt.labelmanager.distribution.agreement.domain.PricingAgreement;
import org.omt.labelmanager.distribution.agreement.infrastructure.PricingAgreementEntity;
import org.omt.labelmanager.distribution.agreement.infrastructure.PricingAgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
class UpdateAgreementUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateAgreementUseCase.class);

    private final PricingAgreementRepository repository;

    UpdateAgreementUseCase(PricingAgreementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PricingAgreement execute(Long agreementId, BigDecimal unitPrice, BigDecimal commissionPercentage) {
        AgreementValidator.validateUnitPrice(unitPrice);
        AgreementValidator.validateCommissionPercentage(commissionPercentage);

        PricingAgreementEntity entity = repository.findById(agreementId)
                .orElseThrow(() -> new AgreementNotFoundException(agreementId));

        entity.setUnitPrice(unitPrice);
        entity.setCommissionPercentage(commissionPercentage);
        entity = repository.save(entity);
        log.info("Updated pricing agreement {}", agreementId);

        return PricingAgreement.fromEntity(entity);
    }

}
