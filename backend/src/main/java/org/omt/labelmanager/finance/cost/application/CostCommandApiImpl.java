package org.omt.labelmanager.finance.cost.application;

import org.omt.labelmanager.finance.cost.infrastructure.CostRepository;
import org.omt.labelmanager.finance.cost.infrastructure.CostEntity;
import org.omt.labelmanager.finance.cost.infrastructure.CostOwnerEmbeddable;
import org.omt.labelmanager.finance.cost.CostMapper;

import org.omt.labelmanager.finance.cost.api.CostCommandApi;
import org.omt.labelmanager.finance.cost.domain.CostOwner;
import org.omt.labelmanager.finance.cost.domain.CostType;
import org.omt.labelmanager.finance.cost.domain.VatAmount;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.finance.shared.DocumentUpload;
import org.omt.labelmanager.finance.shared.RetrievedDocument;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
class CostCommandApiImpl implements CostCommandApi {

    private final RegisterCostUseCase registerCostUseCase;
    private final UpdateCostUseCase updateCostUseCase;
    private final DeleteCostUseCase deleteCostUseCase;
    private final RetrieveCostDocumentUseCase retrieveCostDocumentUseCase;

    CostCommandApiImpl(
            RegisterCostUseCase registerCostUseCase,
            UpdateCostUseCase updateCostUseCase,
            DeleteCostUseCase deleteCostUseCase,
            RetrieveCostDocumentUseCase retrieveCostDocumentUseCase
    ) {
        this.registerCostUseCase = registerCostUseCase;
        this.updateCostUseCase = updateCostUseCase;
        this.deleteCostUseCase = deleteCostUseCase;
        this.retrieveCostDocumentUseCase = retrieveCostDocumentUseCase;
    }

    @Override
    public void registerCost(
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            CostOwner owner,
            String documentReference
    ) {
        registerCostUseCase.registerCost(
                netAmount,
                vat,
                grossAmount,
                type,
                incurredOn,
                description,
                owner,
                documentReference
        );
    }

    @Override
    public void registerCost(
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            CostOwner owner,
            String documentReference,
            DocumentUpload document
    ) {
        registerCostUseCase.registerCost(
                netAmount,
                vat,
                grossAmount,
                type,
                incurredOn,
                description,
                owner,
                documentReference,
                document
        );
    }

    @Override
    public boolean updateCost(
            Long costId,
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            String documentReference
    ) {
        return updateCostUseCase.updateCost(
                costId,
                netAmount,
                vat,
                grossAmount,
                type,
                incurredOn,
                description,
                documentReference
        );
    }

    @Override
    public boolean updateCost(
            Long costId,
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            String documentReference,
            DocumentUpload document
    ) {
        return updateCostUseCase.updateCost(
                costId,
                netAmount,
                vat,
                grossAmount,
                type,
                incurredOn,
                description,
                documentReference,
                document
        );
    }

    @Override
    public boolean deleteCost(Long costId) {
        return deleteCostUseCase.deleteCost(costId);
    }

    @Override
    public Optional<RetrievedDocument> retrieveDocument(Long costId) {
        return retrieveCostDocumentUseCase.retrieveDocument(costId);
    }
}
