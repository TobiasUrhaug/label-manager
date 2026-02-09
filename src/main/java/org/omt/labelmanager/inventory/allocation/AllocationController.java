package org.omt.labelmanager.inventory.allocation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/labels/{labelId}/releases/{releaseId}/production-runs/{runId}/allocations")
public class AllocationController {

    private final AllocateProductionRunToSalesChannelUseCase allocateProductionRunToSalesChannelUseCase;

    public AllocationController(AllocateProductionRunToSalesChannelUseCase allocateProductionRunToSalesChannelUseCase) {
        this.allocateProductionRunToSalesChannelUseCase = allocateProductionRunToSalesChannelUseCase;
    }

    @PostMapping
    public String addAllocation(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long runId,
            @ModelAttribute AddAllocationForm form,
            RedirectAttributes redirectAttributes
    ) {
        try {
            allocateProductionRunToSalesChannelUseCase.invoke(runId, form.getSalesChannelId(), form.getQuantity());
        } catch (InsufficientInventoryException ex) {
            redirectAttributes.addFlashAttribute("allocationError", ex.getMessage());
        }
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }
}
