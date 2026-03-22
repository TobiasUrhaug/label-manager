package org.omt.labelmanager.finance.cost.application;

import org.omt.labelmanager.finance.cost.infrastructure.CostRepository;
import org.omt.labelmanager.finance.cost.infrastructure.CostEntity;
import org.omt.labelmanager.finance.cost.infrastructure.CostOwnerEmbeddable;
import org.omt.labelmanager.finance.cost.CostMapper;

import org.omt.labelmanager.finance.shared.DocumentUpload;
import org.omt.labelmanager.finance.cost.domain.CostType;
import org.omt.labelmanager.finance.cost.domain.VatAmount;
import org.omt.labelmanager.infrastructure.storage.DocumentStoragePort;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
class UpdateCostUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateCostUseCase.class);

    private final CostRepository costRepository;
    private final DocumentStoragePort documentStorage;

    public UpdateCostUseCase(CostRepository costRepository, DocumentStoragePort documentStorage) {
        this.costRepository = costRepository;
        this.documentStorage = documentStorage;
    }

    @Transactional
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
        return updateCost(costId, netAmount, vat, grossAmount, type, incurredOn, description,
                documentReference, null);
    }

    @Transactional
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
        return costRepository.findById(costId)
                .map(cost -> {
                    log.info("Updating cost {}", costId);
                    cost.update(
                            netAmount.amount(),
                            vat.amount().amount(),
                            vat.rate(),
                            grossAmount.amount(),
                            type,
                            incurredOn,
                            description,
                            documentReference
                    );
                    replaceDocument(cost, document);
                    log.debug("Cost {} updated", costId);
                    return true;
                })
                .orElse(false);
    }

    private void replaceDocument(CostEntity cost, DocumentUpload newDocument) {
        if (newDocument == null) {
            return;
        }

        // Delete old document if exists
        if (cost.getDocumentStorageKey() != null) {
            log.info("Deleting old document '{}'", cost.getDocumentStorageKey());
            documentStorage.delete(cost.getDocumentStorageKey());
        }

        // Store new document
        String newKey = documentStorage.store(
                newDocument.filename(),
                newDocument.contentType(),
                newDocument.content()
        );
        cost.setDocumentStorageKey(newKey);
        log.info("New document stored with key '{}'", newKey);
    }
}
