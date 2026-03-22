package org.omt.labelmanager.distribution.agreement.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingAgreementRepository extends JpaRepository<PricingAgreementEntity, Long> {

    List<PricingAgreementEntity> findByDistributorId(Long distributorId);

    boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId);
}
