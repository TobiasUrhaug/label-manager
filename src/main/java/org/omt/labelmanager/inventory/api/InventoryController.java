package org.omt.labelmanager.inventory.api;

import org.omt.labelmanager.inventory.application.InventoryCRUDHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/labels/{labelId}/releases/{releaseId}/inventory")
public class InventoryController {

    private final InventoryCRUDHandler inventoryCRUDHandler;

    public InventoryController(InventoryCRUDHandler inventoryCRUDHandler) {
        this.inventoryCRUDHandler = inventoryCRUDHandler;
    }

    @PostMapping
    public String addInventory(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @ModelAttribute AddInventoryForm form
    ) {
        inventoryCRUDHandler.create(
                releaseId,
                form.getFormat(),
                form.getDescription(),
                form.getManufacturer(),
                form.getManufacturingDate(),
                form.getQuantity()
        );
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }

    @DeleteMapping("/{inventoryId}")
    public String deleteInventory(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long inventoryId
    ) {
        inventoryCRUDHandler.delete(inventoryId);
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }
}
