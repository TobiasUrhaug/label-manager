package org.omt.labelmanager.distribution.agreement.api;

import org.omt.labelmanager.distribution.agreement.PricingAgreement;

import java.util.List;
import java.util.Optional;

public interface AgreementQueryApi {

    Optional<PricingAgreement> findById(Long id);

    List<PricingAgreement> findByDistributorId(Long distributorId);

    boolean existsByDistributorIdAndProductionRunId(Long distributorId, Long productionRunId);
}
