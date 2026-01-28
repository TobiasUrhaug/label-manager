package org.omt.labelmanager.finance.application;

import java.time.LocalDate;
import org.omt.labelmanager.finance.domain.cost.CostType;
import org.omt.labelmanager.finance.domain.cost.VatAmount;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.finance.infrastructure.persistence.cost.CostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCostUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateCostUseCase.class);

    private final CostRepository costRepository;

    public UpdateCostUseCase(CostRepository costRepository) {
        this.costRepository = costRepository;
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
                    log.debug("Cost {} updated", costId);
                    return true;
                })
                .orElse(false);
    }
}
