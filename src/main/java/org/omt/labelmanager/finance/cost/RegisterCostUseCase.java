package org.omt.labelmanager.finance.cost;

import java.time.LocalDate;
import org.omt.labelmanager.common.Money;
import org.omt.labelmanager.finance.cost.persistence.CostEntity;
import org.omt.labelmanager.finance.cost.persistence.CostOwnerEmbeddable;
import org.omt.labelmanager.finance.cost.persistence.CostRepository;
import org.omt.labelmanager.catalog.label.persistence.LabelRepository;
import org.omt.labelmanager.catalog.release.persistence.ReleaseRepository;
import org.omt.labelmanager.identity.user.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterCostUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterCostUseCase.class);

    private final CostRepository costRepository;
    private final ReleaseRepository releaseRepository;
    private final LabelRepository labelRepository;
    private final UserRepository userRepository;

    public RegisterCostUseCase(
            CostRepository costRepository,
            ReleaseRepository releaseRepository,
            LabelRepository labelRepository,
            UserRepository userRepository
    ) {
        this.costRepository = costRepository;
        this.releaseRepository = releaseRepository;
        this.labelRepository = labelRepository;
        this.userRepository = userRepository;
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
        log.info("Registering {} cost for {} {}", type, owner.type(), owner.id());

        validateOwnerExists(owner);

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
                documentReference
        );

        costRepository.save(entity);
        log.debug("Cost registered successfully");
    }

    private void validateOwnerExists(CostOwner owner) {
        boolean exists = switch (owner.type()) {
            case RELEASE -> releaseRepository.existsById(owner.id());
            case LABEL -> labelRepository.existsById(owner.id());
            case USER -> userRepository.existsById(owner.id());
        };

        if (!exists) {
            log.warn("Cannot register cost: {} {} not found", owner.type(), owner.id());
            throw new IllegalArgumentException(owner.type() + " not found: " + owner.id());
        }
    }
}
