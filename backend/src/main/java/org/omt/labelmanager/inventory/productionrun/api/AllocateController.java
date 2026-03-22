package org.omt.labelmanager.inventory.productionrun.api;

import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.LocationType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/labels/{labelId}/releases/{releaseId}/production-runs/{runId}")
public class AllocateController {

    private final ProductionRunCommandApi productionRunCommandApi;

    public AllocateController(ProductionRunCommandApi productionRunCommandApi) {
        this.productionRunCommandApi = productionRunCommandApi;
    }

    @PostMapping("/allocations")
    public String allocate(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long runId,
            @ModelAttribute AllocateForm form,
            RedirectAttributes redirectAttributes
    ) {
        String redirectUrl = "redirect:/labels/" + labelId + "/releases/" + releaseId;
        if (form.getQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("allocationError", "Quantity must be greater than zero");
            return redirectUrl;
        }
        if (form.getLocationType() == null) {
            redirectAttributes.addFlashAttribute("allocationError", "A location type must be selected");
            return redirectUrl;
        }
        if (form.getLocationType() == LocationType.DISTRIBUTOR && form.getDistributorId() == null) {
            redirectAttributes.addFlashAttribute("allocationError", "A distributor must be selected");
            return redirectUrl;
        }
        try {
            productionRunCommandApi.allocate(runId, resolveToLocation(form), form.getQuantity());
        } catch (InsufficientInventoryException ex) {
            redirectAttributes.addFlashAttribute("allocationError", ex.getMessage());
        }
        return redirectUrl;
    }

    @PostMapping("/bandcamp-cancellations")
    public String cancelBandcampReservation(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long runId,
            @ModelAttribute CancelBandcampReservationForm form,
            RedirectAttributes redirectAttributes
    ) {
        String redirectUrl = "redirect:/labels/" + labelId + "/releases/" + releaseId;
        if (form.getQuantity() <= 0) {
            redirectAttributes.addFlashAttribute("cancellationError", "Quantity must be greater than zero");
            return redirectUrl;
        }
        try {
            productionRunCommandApi.cancelBandcampReservation(runId, form.getQuantity());
        } catch (InsufficientInventoryException ex) {
            redirectAttributes.addFlashAttribute("cancellationError", ex.getMessage());
        }
        return redirectUrl;
    }

    private InventoryLocation resolveToLocation(AllocateForm form) {
        if (form.getLocationType() == LocationType.BANDCAMP) {
            return InventoryLocation.bandcamp();
        }
        return InventoryLocation.distributor(form.getDistributorId());
    }
}
