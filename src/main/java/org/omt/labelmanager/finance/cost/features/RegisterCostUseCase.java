package org.omt.labelmanager.finance.cost.features;

import org.omt.labelmanager.catalog.label.api.LabelQueryFacade;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryFacade;
import org.omt.labelmanager.finance.cost.DocumentUpload;
import org.omt.labelmanager.finance.cost.CostOwner;
import org.omt.labelmanager.finance.cost.CostType;
import org.omt.labelmanager.finance.cost.VatAmount;
import org.omt.labelmanager.finance.cost.persistence.CostEntity;
import org.omt.labelmanager.finance.cost.persistence.CostOwnerEmbeddable;
import org.omt.labelmanager.finance.cost.persistence.CostRepository;
import org.omt.labelmanager.finance.cost.ports.DocumentStoragePort;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class RegisterCostUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterCostUseCase.class);

    private final CostRepository costRepository;
    private final ReleaseQueryFacade releaseQueryFacade;
    private final LabelQueryFacade labelQueryFacade;
    private final UserRepository userRepository;
    private final DocumentStoragePort documentStorage;

    public RegisterCostUseCase(
            CostRepository costRepository,
            ReleaseQueryFacade releaseQueryFacade,
            LabelQueryFacade labelQueryFacade,
            UserRepository userRepository,
            DocumentStoragePort documentStorage
    ) {
        this.costRepository = costRepository;
        this.releaseQueryFacade = releaseQueryFacade;
        this.labelQueryFacade = labelQueryFacade;
        this.userRepository = userRepository;
        this.documentStorage = documentStorage;
    }

    @Transactional
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
        registerCost(netAmount, vat, grossAmount, type, incurredOn, description, owner,
                documentReference, null);
    }

    @Transactional
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
        log.info("Registering {} cost for {} {}", type, owner.type(), owner.id());

        validateOwnerExists(owner);

        String documentStorageKey = storeDocument(document);

        var entity = new CostEntity(
                netAmount.currency(),
                netAmount.amount(),
                vat.amount().amount(),
                vat.rate(),
                grossAmount.amount(),
                type,
                incurredOn,
                description,
                CostOwnerEmbeddable.fromCostOwner(owner),
                documentReference,
                documentStorageKey
        );

        costRepository.save(entity);
        log.debug("Cost registered successfully");
    }

    private String storeDocument(DocumentUpload document) {
        if (document == null) {
            return null;
        }
        return documentStorage.store(
                document.filename(),
                document.contentType(),
                document.content()
        );
    }

    private void validateOwnerExists(CostOwner owner) {
        boolean exists = switch (owner.type()) {
            case RELEASE -> releaseQueryFacade.exists(owner.id());
            case LABEL -> labelQueryFacade.exists(owner.id());
            case USER -> userRepository.existsById(owner.id());
        };

        if (!exists) {
            log.warn("Cannot register cost: {} {} not found", owner.type(), owner.id());
            throw new IllegalArgumentException(owner.type() + " not found: " + owner.id());
        }
    }
}
