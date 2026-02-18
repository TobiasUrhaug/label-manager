package org.omt.labelmanager.sales.sale.api;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.sales.sale.domain.Sale;
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
    private final DistributorQueryApi distributorQueryApi;

    public SaleController(
            SaleCommandApi saleCommandApi,
            SaleQueryApi saleQueryApi,
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi,
            DistributorQueryApi distributorQueryApi
    ) {
        this.saleCommandApi = saleCommandApi;
        this.saleQueryApi = saleQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
        this.distributorQueryApi = distributorQueryApi;
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
        var distributors = distributorQueryApi.findByLabelId(labelId);

        model.addAttribute("label", label);
        model.addAttribute("releases", releases);
        model.addAttribute("distributors", distributors);
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
                    form.getDistributorId(),
                    form.toLineItemInputs()
            );

            return "redirect:/labels/" + labelId + "/sales";
        } catch (IllegalStateException | IllegalArgumentException
                | InsufficientInventoryException e) {
            var label = labelQueryApi.findById(labelId)
                    .orElseThrow(() -> new EntityNotFoundException("Label not found"));
            var releases = releaseQueryApi.getReleasesForLabel(labelId);
            var distributors = distributorQueryApi.findByLabelId(labelId);

            model.addAttribute("label", label);
            model.addAttribute("releases", releases);
            model.addAttribute("distributors", distributors);
            model.addAttribute("formats", ReleaseFormat.values());
            model.addAttribute("channels", ChannelType.values());
            model.addAttribute("form", form);
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

    @GetMapping("/{saleId}/edit")
    public String showEditForm(
            @PathVariable Long labelId,
            @PathVariable Long saleId,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var sale = saleQueryApi.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        var releases = releaseQueryApi.getReleasesForLabel(labelId);

        model.addAttribute("label", label);
        model.addAttribute("sale", sale);
        model.addAttribute("releases", releases);
        model.addAttribute("formats", ReleaseFormat.values());
        model.addAttribute("form", buildEditForm(sale));

        return "sale/edit";
    }

    @PostMapping("/{saleId}")
    public String submitEdit(
            @PathVariable Long labelId,
            @PathVariable Long saleId,
            EditSaleForm form,
            Model model
    ) {
        try {
            var updated = saleCommandApi.updateSale(
                    saleId,
                    form.getSaleDate(),
                    form.getNotes(),
                    form.toLineItemInputs()
            );
            return "redirect:/labels/" + labelId + "/sales/" + updated.id();
        } catch (IllegalStateException | IllegalArgumentException
                | InsufficientInventoryException e) {
            var label = labelQueryApi.findById(labelId)
                    .orElseThrow(() -> new EntityNotFoundException("Label not found"));
            var sale = saleQueryApi.findById(saleId)
                    .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
            var releases = releaseQueryApi.getReleasesForLabel(labelId);

            model.addAttribute("label", label);
            model.addAttribute("sale", sale);
            model.addAttribute("releases", releases);
            model.addAttribute("formats", ReleaseFormat.values());
            model.addAttribute("form", form);
            model.addAttribute("errorMessage", e.getMessage());

            return "sale/edit";
        }
    }

    @PostMapping("/{saleId}/delete")
    public String deleteSale(
            @PathVariable Long labelId,
            @PathVariable Long saleId
    ) {
        try {
            saleCommandApi.deleteSale(saleId);
        } catch (EntityNotFoundException ignored) {
            // Sale already gone — redirect gracefully
        }
        return "redirect:/labels/" + labelId + "/sales";
    }

    private EditSaleForm buildEditForm(Sale sale) {
        var form = new EditSaleForm();
        form.setSaleDate(sale.saleDate());
        form.setNotes(sale.notes());
        form.setLineItems(
                sale.lineItems().stream()
                        .map(item -> new SaleLineItemForm(
                                item.releaseId(),
                                item.format(),
                                item.quantity(),
                                item.unitPrice().amount()
                        ))
                        .toList()
        );
        return form;
    }
}
