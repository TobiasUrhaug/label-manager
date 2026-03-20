package org.omt.labelmanager.distribution.agreement.api;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.agreement.domain.PricingAgreement;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/labels/{labelId}/distributors/{distributorId}/agreements")
public class AgreementController {

    private final AgreementCommandApi commandApi;
    private final AgreementQueryApi queryApi;
    private final DistributorQueryApi distributorQueryApi;
    private final LabelQueryApi labelQueryApi;
    private final AllocationQueryApi allocationQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final ReleaseQueryApi releaseQueryApi;

    public AgreementController(
            AgreementCommandApi commandApi,
            AgreementQueryApi queryApi,
            DistributorQueryApi distributorQueryApi,
            LabelQueryApi labelQueryApi,
            AllocationQueryApi allocationQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            ReleaseQueryApi releaseQueryApi
    ) {
        this.commandApi = commandApi;
        this.queryApi = queryApi;
        this.distributorQueryApi = distributorQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.allocationQueryApi = allocationQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.releaseQueryApi = releaseQueryApi;
    }

    @GetMapping
    public String listAgreements(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributor = distributorQueryApi.findById(distributorId)
                .filter(d -> d.labelId().equals(labelId))
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        var agreements = queryApi.findByDistributorId(distributorId);
        var enriched = agreements.stream()
                .map(a -> enrichAgreement(a))
                .toList();

        model.addAttribute("label", label);
        model.addAttribute("distributor", distributor);
        model.addAttribute("agreements", enriched);

        return "distributor/agreements";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributor = distributorQueryApi.findById(distributorId)
                .filter(d -> d.labelId().equals(labelId))
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        model.addAttribute("label", label);
        model.addAttribute("distributor", distributor);
        model.addAttribute("form", new AgreementForm());
        model.addAttribute("availableRuns", buildAvailableRuns(distributorId));

        return "distributor/agreement-form";
    }

    @PostMapping
    public String createAgreement(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            @ModelAttribute AgreementForm form,
            Model model
    ) {
        try {
            commandApi.create(distributorId, form.getProductionRunId(),
                    form.getUnitPrice(), form.getCommissionPercentage());
            return "redirect:/labels/" + labelId + "/distributors/" + distributorId + "/agreements";
        } catch (DuplicateAgreementException | IllegalArgumentException e) {
            var label = labelQueryApi.findById(labelId).orElseThrow();
            var distributor = distributorQueryApi.findById(distributorId).orElseThrow();
            model.addAttribute("label", label);
            model.addAttribute("distributor", distributor);
            model.addAttribute("form", form);
            model.addAttribute("availableRuns", buildAvailableRuns(distributorId));
            model.addAttribute("errorMessage", e.getMessage());
            return "distributor/agreement-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            @PathVariable Long id,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributor = distributorQueryApi.findById(distributorId)
                .filter(d -> d.labelId().equals(labelId))
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
        var agreement = queryApi.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agreement not found"));

        var form = new AgreementForm();
        form.setProductionRunId(agreement.productionRunId());
        form.setUnitPrice(agreement.unitPrice());
        form.setCommissionPercentage(agreement.commissionPercentage());

        var runDisplayName = buildDisplayName(agreement.productionRunId());

        model.addAttribute("label", label);
        model.addAttribute("distributor", distributor);
        model.addAttribute("agreement", agreement);
        model.addAttribute("form", form);
        model.addAttribute("productionRunDisplayName", runDisplayName);

        return "distributor/agreement-form";
    }

    @PostMapping("/{id}")
    public String updateAgreement(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            @PathVariable Long id,
            @ModelAttribute AgreementForm form,
            Model model
    ) {
        try {
            commandApi.update(id, form.getUnitPrice(), form.getCommissionPercentage());
            return "redirect:/labels/" + labelId + "/distributors/" + distributorId + "/agreements";
        } catch (IllegalArgumentException e) {
            var label = labelQueryApi.findById(labelId).orElseThrow();
            var distributor = distributorQueryApi.findById(distributorId).orElseThrow();
            var agreement = queryApi.findById(id).orElseThrow();
            model.addAttribute("label", label);
            model.addAttribute("distributor", distributor);
            model.addAttribute("agreement", agreement);
            model.addAttribute("form", form);
            model.addAttribute("productionRunDisplayName", buildDisplayName(form.getProductionRunId()));
            model.addAttribute("errorMessage", e.getMessage());
            return "distributor/agreement-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteAgreement(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            @PathVariable Long id
    ) {
        commandApi.delete(id);
        return "redirect:/labels/" + labelId + "/distributors/" + distributorId + "/agreements";
    }

    private List<AvailableProductionRunView> buildAvailableRuns(Long distributorId) {
        var allocatedRunIds = allocationQueryApi.getAllocationsForDistributor(distributorId)
                .stream()
                .map(a -> a.productionRunId())
                .toList();

        return allocatedRunIds.stream()
                .filter(runId -> !queryApi.existsByDistributorIdAndProductionRunId(distributorId, runId))
                .map(runId -> new AvailableProductionRunView(runId, buildDisplayName(runId)))
                .toList();
    }

    private String buildDisplayName(Long productionRunId) {
        return productionRunQueryApi.findById(productionRunId)
                .map(run -> {
                    var title = releaseQueryApi.findById(run.releaseId())
                            .map(r -> r.name())
                            .orElse("Unknown Release");
                    return title + " \u2013 " + run.format();
                })
                .orElse("Unknown");
    }

    private AgreementView enrichAgreement(PricingAgreement agreement) {
        var displayName = buildDisplayName(agreement.productionRunId());
        return new AgreementView(agreement, displayName);
    }

    public record AgreementView(PricingAgreement agreement, String productionRunDisplayName) {
    }
}
