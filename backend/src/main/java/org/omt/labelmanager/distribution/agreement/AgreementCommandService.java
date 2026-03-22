package org.omt.labelmanager.distribution.agreement;

import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementNotFoundException;
import org.omt.labelmanager.distribution.agreement.api.DuplicateAgreementException;
import org.omt.labelmanager.distribution.agreement.persistence.PricingAgreementEntity;
import org.omt.labelmanager.distribution.agreement.persistence.PricingAgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
class AgreementCommandService implements AgreementCommandApi {

    private static final Logger log = LoggerFactory.getLogger(AgreementCommandService.class);

    private final PricingAgreementRepository repository;

    AgreementCommandService(PricingAgreementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PricingAgreement create(
            Long distributorId,
            Long productionRunId,
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue
    ) {
        if (repository.existsByDistributorIdAndProductionRunId(distributorId, productionRunId)) {
            throw new DuplicateAgreementException(distributorId, productionRunId);
        }

        new PricingAgreement(null, distributorId, productionRunId, unitPrice, commissionType, commissionValue, null);

        PricingAgreementEntity entity =
                new PricingAgreementEntity(distributorId, productionRunId, unitPrice, commissionType, commissionValue);
        entity = repository.save(entity);
        log.info("Created pricing agreement {} for distributor {} and production run {}",
                entity.getId(), distributorId, productionRunId);

        return PricingAgreement.fromEntity(entity);
    }

    @Override
    @Transactional
    public PricingAgreement update(Long agreementId, BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue) {
        PricingAgreementEntity entity = repository.findById(agreementId)
                .orElseThrow(() -> new AgreementNotFoundException(agreementId));

        new PricingAgreement(entity.getId(), entity.getDistributorId(), entity.getProductionRunId(),
                unitPrice, commissionType, commissionValue, entity.getCreatedAt());

        entity.setUnitPrice(unitPrice);
        entity.setCommissionType(commissionType);
        entity.setCommissionValue(commissionValue);
        entity = repository.save(entity);
        log.info("Updated pricing agreement {}", agreementId);

        return PricingAgreement.fromEntity(entity);
    }

    @Override
    @Transactional
    public void delete(Long agreementId) {
        if (!repository.existsById(agreementId)) {
            throw new AgreementNotFoundException(agreementId);
        }

        repository.deleteById(agreementId);
        log.info("Deleted pricing agreement {}", agreementId);
    }
}
