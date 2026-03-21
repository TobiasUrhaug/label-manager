package org.omt.labelmanager.distribution.agreement.application;

import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.domain.CommissionType;
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
            CommissionType commissionType,
            BigDecimal commissionValue
    ) {
        return createAgreement.execute(distributorId, productionRunId, unitPrice, commissionType, commissionValue);
    }

    @Override
    public PricingAgreement update(Long agreementId, BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue) {
        return updateAgreement.execute(agreementId, unitPrice, commissionType, commissionValue);
    }

    @Override
    public void delete(Long agreementId) {
        deleteAgreement.execute(agreementId);
    }
}
