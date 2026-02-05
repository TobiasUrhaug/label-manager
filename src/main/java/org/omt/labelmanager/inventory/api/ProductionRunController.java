package org.omt.labelmanager.inventory.api;

import org.omt.labelmanager.inventory.application.ProductionRunCRUDHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/labels/{labelId}/releases/{releaseId}/production-runs")
public class ProductionRunController {

    private final ProductionRunCRUDHandler productionRunCRUDHandler;

    public ProductionRunController(ProductionRunCRUDHandler productionRunCRUDHandler) {
        this.productionRunCRUDHandler = productionRunCRUDHandler;
    }

    @PostMapping
    public String addProductionRun(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @ModelAttribute AddProductionRunForm form
    ) {
        productionRunCRUDHandler.create(
                releaseId,
                form.getFormat(),
                form.getDescription(),
                form.getManufacturer(),
                form.getManufacturingDate(),
                form.getQuantity()
        );
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }

    @DeleteMapping("/{productionRunId}")
    public String deleteProductionRun(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long productionRunId
    ) {
        productionRunCRUDHandler.delete(productionRunId);
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }
}
