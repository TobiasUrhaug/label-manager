package org.omt.labelmanager.distribution.agreement.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingAgreementRepository extends JpaRepository<PricingAgreementEntity, Long> {

    List<PricingAgreementEntity> findByDistributorId(Long distributorId);

    boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId);
}
