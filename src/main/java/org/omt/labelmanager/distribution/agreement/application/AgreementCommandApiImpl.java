package org.omt.labelmanager.distribution.agreement.application;

import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.domain.PricingAgreement;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
class AgreementCommandApiImpl implements AgreementCommandApi {

    private final CreateAgreementUseCase createAgreement;
    private final UpdateAgreementUseCase updateAgreement;
    private final DeleteAgreementUseCase deleteAgreement;

    AgreementCommandApiImpl(
            CreateAgreementUseCase createAgreement,
            UpdateAgreementUseCase updateAgreement,
            DeleteAgreementUseCase deleteAgreement
    ) {
        this.createAgreement = createAgreement;
        this.updateAgreement = updateAgreement;
        this.deleteAgreement = deleteAgreement;
    }

    @Override
    public PricingAgreement create(
            Long distributorId,
            Long productionRunId,
            BigDecimal unitPrice,
            BigDecimal commissionPercentage
    ) {
        return createAgreement.execute(distributorId, productionRunId, unitPrice, commissionPercentage);
    }

    @Override
    public PricingAgreement update(Long agreementId, BigDecimal unitPrice, BigDecimal commissionPercentage) {
        return updateAgreement.execute(agreementId, unitPrice, commissionPercentage);
    }

    @Override
    public void delete(Long agreementId) {
        deleteAgreement.execute(agreementId);
    }
}
