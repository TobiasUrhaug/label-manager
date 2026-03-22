package org.omt.labelmanager.distribution.agreement;

import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.agreement.persistence.PricingAgreementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
class AgreementQueryService implements AgreementQueryApi {

    private final PricingAgreementRepository repository;

    AgreementQueryService(PricingAgreementRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PricingAgreement> findById(Long id) {
        return repository.findById(id).map(PricingAgreement::fromEntity);
    }

    @Override
    public List<PricingAgreement> findByDistributorId(Long distributorId) {
        return repository.findByDistributorId(distributorId).stream()
                .map(PricingAgreement::fromEntity)
                .toList();
    }

    @Override
    public boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId) {
        return repository.existsByDistributorIdAndProductionRunId(distributorId, productionRunId);
    }
}
