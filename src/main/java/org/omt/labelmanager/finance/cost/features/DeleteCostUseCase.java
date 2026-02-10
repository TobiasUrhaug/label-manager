package org.omt.labelmanager.finance.cost.features;

import org.omt.labelmanager.finance.cost.persistence.CostEntity;
import org.omt.labelmanager.finance.cost.persistence.CostRepository;
import org.omt.labelmanager.finance.cost.ports.DocumentStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCostUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteCostUseCase.class);

    private final CostRepository costRepository;
    private final DocumentStoragePort documentStorage;

    public DeleteCostUseCase(CostRepository costRepository, DocumentStoragePort documentStorage) {
        this.costRepository = costRepository;
        this.documentStorage = documentStorage;
    }

    @Transactional
    public boolean deleteCost(Long costId) {
        return costRepository.findById(costId)
                .map(this::deleteWithDocument)
                .orElse(false);
    }

    private boolean deleteWithDocument(CostEntity cost) {
        log.info("Deleting cost {}", cost.getId());

        if (cost.getDocumentStorageKey() != null) {
            log.info("Deleting attached document '{}'", cost.getDocumentStorageKey());
            documentStorage.delete(cost.getDocumentStorageKey());
        }

        costRepository.delete(cost);
        log.debug("Cost {} deleted", cost.getId());
        return true;
    }
}
