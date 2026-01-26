package org.omt.labelmanager.finance.cost.api;

import org.omt.labelmanager.finance.domain.cost.CostOwner;
import org.omt.labelmanager.finance.cost.RegisterCostUseCase;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CostController {

    private final RegisterCostUseCase registerCostUseCase;

    public CostController(RegisterCostUseCase registerCostUseCase) {
        this.registerCostUseCase = registerCostUseCase;
    }

    @PostMapping("/labels/{labelId}/releases/{releaseId}/costs")
    public String registerCostForRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            RegisterCostForm form
    ) {
        registerCostUseCase.registerCost(
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                CostOwner.release(releaseId),
                form.getDocumentReference()
        );
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }

    @PostMapping("/labels/{labelId}/costs")
    public String registerCostForLabel(
            @PathVariable Long labelId,
            RegisterCostForm form
    ) {
        registerCostUseCase.registerCost(
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                CostOwner.label(labelId),
                form.getDocumentReference()
        );
        return "redirect:/labels/" + labelId;
    }
}
