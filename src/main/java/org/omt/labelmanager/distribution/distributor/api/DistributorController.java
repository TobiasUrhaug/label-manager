package org.omt.labelmanager.distribution.distributor.api;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/labels/{labelId}/distributors")
public class DistributorController {

    private final DistributorCommandApi commandApi;
    private final DistributorQueryApi distributorQueryApi;
    private final LabelQueryApi labelQueryApi;
    private final SaleQueryApi saleQueryApi;
    private final DistributorReturnQueryApi returnQueryApi;

    public DistributorController(
            DistributorCommandApi commandApi,
            DistributorQueryApi distributorQueryApi,
            LabelQueryApi labelQueryApi,
            SaleQueryApi saleQueryApi,
            DistributorReturnQueryApi returnQueryApi
    ) {
        this.commandApi = commandApi;
        this.distributorQueryApi = distributorQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.saleQueryApi = saleQueryApi;
        this.returnQueryApi = returnQueryApi;
    }

    @GetMapping("/{distributorId}")
    public String showDistributor(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributor = distributorQueryApi.findById(distributorId)
                .filter(d -> d.labelId().equals(labelId))
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
        var sales = saleQueryApi.getSalesForDistributor(distributorId);
        var returns = returnQueryApi.getReturnsForDistributor(distributorId);

        model.addAttribute("label", label);
        model.addAttribute("distributor", distributor);
        model.addAttribute("sales", sales);
        model.addAttribute("returns", returns);

        return "distributor/detail";
    }

    @PostMapping
    public String addDistributor(
            @PathVariable Long labelId,
            @ModelAttribute AddDistributorForm form
    ) {
        commandApi.createDistributor(
                labelId,
                form.getName(),
                form.getChannelType()
        );
        return "redirect:/labels/" + labelId;
    }

    @DeleteMapping("/{distributorId}")
    public String deleteDistributor(
            @PathVariable Long labelId,
            @PathVariable Long distributorId
    ) {
        commandApi.delete(distributorId);
        return "redirect:/labels/" + labelId;
    }
}
