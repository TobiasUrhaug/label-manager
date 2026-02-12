package org.omt.labelmanager.sales.sale.api;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/labels/{labelId}/sales")
public class SaleController {

    private final SaleCommandApi saleCommandApi;
    private final SaleQueryApi saleQueryApi;
    private final LabelQueryApi labelQueryApi;
    private final ReleaseQueryApi releaseQueryApi;

    public SaleController(
            SaleCommandApi saleCommandApi,
            SaleQueryApi saleQueryApi,
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi
    ) {
        this.saleCommandApi = saleCommandApi;
        this.saleQueryApi = saleQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
    }

    @GetMapping
    public String listSales(@PathVariable Long labelId, Model model) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var sales = saleQueryApi.getSalesForLabel(labelId);
        var totalRevenue = saleQueryApi.getTotalRevenueForLabel(labelId);

        model.addAttribute("label", label);
        model.addAttribute("sales", sales);
        model.addAttribute("totalRevenue", totalRevenue);

        return "sale/list";
    }

    @GetMapping("/new")
    public String showRegisterForm(@PathVariable Long labelId, Model model) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var releases = releaseQueryApi.getReleasesForLabel(labelId);

        model.addAttribute("label", label);
        model.addAttribute("releases", releases);
        model.addAttribute("formats", ReleaseFormat.values());
        model.addAttribute("channels", ChannelType.values());
        model.addAttribute("form", new RegisterSaleForm());

        return "sale/register";
    }

    @PostMapping
    public String registerSale(
            @PathVariable Long labelId,
            RegisterSaleForm form,
            Model model
    ) {
        try {
            saleCommandApi.registerSale(
                    labelId,
                    form.getSaleDate(),
                    form.getChannel(),
                    form.getNotes(),
                    form.toLineItemInputs()
            );

            return "redirect:/labels/" + labelId + "/sales";
        } catch (IllegalStateException e) {
            var label = labelQueryApi.findById(labelId)
                    .orElseThrow(() -> new EntityNotFoundException("Label not found"));
            var releases = releaseQueryApi.getReleasesForLabel(labelId);

            model.addAttribute("label", label);
            model.addAttribute("releases", releases);
            model.addAttribute("formats", ReleaseFormat.values());
            model.addAttribute("channels", ChannelType.values());
            model.addAttribute("errorMessage", e.getMessage());

            return "sale/register";
        }
    }

    @GetMapping("/{saleId}")
    public String viewSale(
            @PathVariable Long labelId,
            @PathVariable Long saleId,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var sale = saleQueryApi.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found"));

        // Enrich line items with release names
        var releaseNames = sale.lineItems().stream()
                .map(item -> {
                    var release = releaseQueryApi.findById(item.releaseId());
                    return release.map(r -> r.name()).orElse("Unknown");
                })
                .toList();

        model.addAttribute("label", label);
        model.addAttribute("sale", sale);
        model.addAttribute("releaseNames", releaseNames);

        return "sale/detail";
    }
}
