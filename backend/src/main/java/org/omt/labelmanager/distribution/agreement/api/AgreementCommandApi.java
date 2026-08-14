package org.omt.labelmanager.distribution.agreement.api;

import java.math.BigDecimal;

public interface AgreementCommandApi {

    PricingAgreement create(
            Long distributorId,
            Long productionRunId,
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue);

    PricingAgreement update(
            Long agreementId,
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue);

    void delete(Long agreementId);
}
