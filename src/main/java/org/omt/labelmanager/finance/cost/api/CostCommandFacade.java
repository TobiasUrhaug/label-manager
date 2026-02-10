package org.omt.labelmanager.finance.cost.api;

import org.omt.labelmanager.finance.cost.DocumentUpload;
import org.omt.labelmanager.finance.cost.RetrievedDocument;
import org.omt.labelmanager.finance.cost.CostOwner;
import org.omt.labelmanager.finance.cost.CostType;
import org.omt.labelmanager.finance.cost.VatAmount;
import org.omt.labelmanager.finance.domain.shared.Money;

import java.time.LocalDate;
import java.util.Optional;

public interface CostCommandFacade {

    void registerCost(
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            CostOwner owner,
            String documentReference
    );

    void registerCost(
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            CostOwner owner,
            String documentReference,
            DocumentUpload document
    );

    boolean updateCost(
            Long costId,
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            String documentReference
    );

    boolean updateCost(
            Long costId,
            Money netAmount,
            VatAmount vat,
            Money grossAmount,
            CostType type,
            LocalDate incurredOn,
            String description,
            String documentReference,
            DocumentUpload document
    );

    boolean deleteCost(Long costId);

    Optional<RetrievedDocument> retrieveDocument(Long costId);

}
