package org.omt.labelmanager.inventory.allocation.api;

import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/labels/{labelId}/releases/{releaseId}/production-runs/{runId}/allocations")
public class AllocationController {

    private final AllocationCommandApi allocationCommandApi;

    public AllocationController(AllocationCommandApi allocationCommandApi) {
        this.allocationCommandApi = allocationCommandApi;
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
            allocationCommandApi.createAllocation(
                    runId,
                    form.getDistributorId(),
                    form.getQuantity()
            );
        } catch (InsufficientInventoryException ex) {
            redirectAttributes.addFlashAttribute("allocationError", ex.getMessage());
        }
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }
}
