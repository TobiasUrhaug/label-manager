package org.omt.labelmanager.inventory.api;

import org.omt.labelmanager.inventory.application.AllocationCRUDHandler;
import org.omt.labelmanager.inventory.application.InsufficientInventoryException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/labels/{labelId}/releases/{releaseId}/production-runs/{runId}/allocations")
public class AllocationController {

    private final AllocationCRUDHandler allocationCRUDHandler;

    public AllocationController(AllocationCRUDHandler allocationCRUDHandler) {
        this.allocationCRUDHandler = allocationCRUDHandler;
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
            allocationCRUDHandler.create(runId, form.getSalesChannelId(), form.getQuantity());
        } catch (InsufficientInventoryException ex) {
            redirectAttributes.addFlashAttribute("allocationError", ex.getMessage());
        }
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }
}
