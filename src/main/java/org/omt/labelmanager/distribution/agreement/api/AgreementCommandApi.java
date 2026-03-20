package org.omt.labelmanager.distribution.agreement.api;

import org.omt.labelmanager.distribution.agreement.domain.PricingAgreement;

import java.math.BigDecimal;

public interface AgreementCommandApi {

    PricingAgreement create(Long distributorId, Long productionRunId,
                            BigDecimal unitPrice, BigDecimal commissionPercentage);

    PricingAgreement update(Long agreementId,
                            BigDecimal unitPrice, BigDecimal commissionPercentage);

    void delete(Long agreementId);
}
