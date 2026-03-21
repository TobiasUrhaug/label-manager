package org.omt.labelmanager.distribution.agreement.application;

import org.omt.labelmanager.distribution.agreement.api.AgreementNotFoundException;
import org.omt.labelmanager.distribution.agreement.domain.CommissionType;
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
    public PricingAgreement execute(Long agreementId, BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue) {
        PricingAgreementEntity entity = repository.findById(agreementId)
                .orElseThrow(() -> new AgreementNotFoundException(agreementId));

        entity.setUnitPrice(unitPrice);
        entity.setCommissionType(commissionType);
        entity.setCommissionValue(commissionValue);
        entity = repository.save(entity);
        log.info("Updated pricing agreement {}", agreementId);

        return PricingAgreement.fromEntity(entity);
    }

}
