package org.omt.labelmanager.inventory.productionrun.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/labels/{labelId}/releases/{releaseId}/production-runs")
public class ProductionRunController {

    private final ProductionRunCommandApi commandApi;

    public ProductionRunController(ProductionRunCommandApi commandApi) {
        this.commandApi = commandApi;
    }

    @PostMapping
    public String addProductionRun(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @ModelAttribute AddProductionRunForm form
    ) {
        commandApi.createProductionRun(
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
        commandApi.delete(productionRunId);
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }
}
