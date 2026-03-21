package org.omt.labelmanager.distribution.agreement.application;

import org.omt.labelmanager.distribution.agreement.api.DuplicateAgreementException;
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
class CreateAgreementUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateAgreementUseCase.class);

    private final PricingAgreementRepository repository;

    CreateAgreementUseCase(PricingAgreementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PricingAgreement execute(
            Long distributorId,
            Long productionRunId,
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue
    ) {
        AgreementValidator.validateUnitPrice(unitPrice);
        AgreementValidator.validateCommissionValue(commissionType, commissionValue);

        if (repository.existsByDistributorIdAndProductionRunId(distributorId, productionRunId)) {
            throw new DuplicateAgreementException(distributorId, productionRunId);
        }

        PricingAgreementEntity entity =
                new PricingAgreementEntity(distributorId, productionRunId, unitPrice, commissionType, commissionValue);
        entity = repository.save(entity);
        log.info("Created pricing agreement {} for distributor {} and production run {}",
                entity.getId(), distributorId, productionRunId);

        return PricingAgreement.fromEntity(entity);
    }

}
