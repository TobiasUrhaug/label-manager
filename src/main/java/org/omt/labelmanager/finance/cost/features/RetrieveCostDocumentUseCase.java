package org.omt.labelmanager.finance.cost.features;

import org.omt.labelmanager.finance.cost.RetrievedDocument;
import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.finance.cost.persistence.CostRepository;
import org.omt.labelmanager.finance.cost.ports.DocumentStoragePort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RetrieveCostDocumentUseCase {

    private final CostRepository costRepository;
    private final DocumentStoragePort documentStorage;

    public RetrieveCostDocumentUseCase(
            CostRepository costRepository,
            DocumentStoragePort documentStorage
    ) {
        this.costRepository = costRepository;
        this.documentStorage = documentStorage;
    }

    public Optional<RetrievedDocument> retrieveDocument(Long costId) {
        return costRepository.findById(costId)
                .map(Cost::fromEntity)
                .filter(cost -> cost.documentStorageKey() != null)
                .map(cost -> documentStorage.retrieve(cost.documentStorageKey()));
    }
}
