package org.omt.labelmanager.inventory.api;

import org.omt.labelmanager.inventory.application.SalesChannelCRUDHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/labels/{labelId}/sales-channels")
public class SalesChannelController {

    private final SalesChannelCRUDHandler salesChannelCRUDHandler;

    public SalesChannelController(SalesChannelCRUDHandler salesChannelCRUDHandler) {
        this.salesChannelCRUDHandler = salesChannelCRUDHandler;
    }

    @PostMapping
    public String addSalesChannel(
            @PathVariable Long labelId,
            @ModelAttribute AddSalesChannelForm form
    ) {
        salesChannelCRUDHandler.create(labelId, form.getName(), form.getChannelType());
        return "redirect:/labels/" + labelId;
    }

    @DeleteMapping("/{channelId}")
    public String deleteSalesChannel(
            @PathVariable Long labelId,
            @PathVariable Long channelId
    ) {
        salesChannelCRUDHandler.delete(channelId);
        return "redirect:/labels/" + labelId;
    }
}
