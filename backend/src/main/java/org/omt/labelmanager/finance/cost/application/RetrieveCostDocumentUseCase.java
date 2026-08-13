package org.omt.labelmanager.finance.cost.application;

import java.util.Optional;
import org.omt.labelmanager.finance.cost.CostMapper;
import org.omt.labelmanager.finance.cost.infrastructure.CostRepository;
import org.omt.labelmanager.finance.shared.DocumentStoragePort;
import org.omt.labelmanager.finance.shared.RetrievedDocument;
import org.springframework.stereotype.Service;

@Service
class RetrieveCostDocumentUseCase {

    private final CostRepository costRepository;
    private final DocumentStoragePort documentStorage;

    public RetrieveCostDocumentUseCase(
            CostRepository costRepository, DocumentStoragePort documentStorage) {
        this.costRepository = costRepository;
        this.documentStorage = documentStorage;
    }

    public Optional<RetrievedDocument> retrieveDocument(Long costId) {
        return costRepository
                .findById(costId)
                .map(CostMapper::fromEntity)
                .filter(cost -> cost.documentStorageKey() != null)
                .map(cost -> documentStorage.retrieve(cost.documentStorageKey()));
    }
}
