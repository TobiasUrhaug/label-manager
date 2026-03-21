package org.omt.labelmanager.distribution.agreement.api;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
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

import org.omt.labelmanager.distribution.agreement.CommissionType;

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
            @PathVariable Long distributorId
    ) {
        return "redirect:/labels/" + labelId + "/distributors/" + distributorId;
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
        model.addAttribute("commissionTypes", CommissionType.values());

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
                    form.getUnitPrice(), form.getCommissionType(), form.getCommissionValue());
            return "redirect:/labels/" + labelId + "/distributors/" + distributorId;
        } catch (DuplicateAgreementException | IllegalArgumentException e) {
            var label = labelQueryApi.findById(labelId).orElseThrow();
            var distributor = distributorQueryApi.findById(distributorId).orElseThrow();
            model.addAttribute("label", label);
            model.addAttribute("distributor", distributor);
            model.addAttribute("form", form);
            model.addAttribute("availableRuns", buildAvailableRuns(distributorId));
            model.addAttribute("commissionTypes", CommissionType.values());
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
                .orElseThrow(() -> new AgreementNotFoundException(id));
        if (!agreement.distributorId().equals(distributorId)) {
            throw new AgreementNotFoundException(id);
        }

        var form = new AgreementForm();
        form.setProductionRunId(agreement.productionRunId());
        form.setUnitPrice(agreement.unitPrice());
        form.setCommissionType(agreement.commissionType());
        form.setCommissionValue(agreement.commissionValue());

        var runDisplayName = buildDisplayName(agreement.productionRunId());

        model.addAttribute("label", label);
        model.addAttribute("distributor", distributor);
        model.addAttribute("agreement", agreement);
        model.addAttribute("form", form);
        model.addAttribute("productionRunDisplayName", runDisplayName);
        model.addAttribute("commissionTypes", CommissionType.values());

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
        var agreement = queryApi.findById(id)
                .orElseThrow(() -> new AgreementNotFoundException(id));
        if (!agreement.distributorId().equals(distributorId)) {
            throw new AgreementNotFoundException(id);
        }

        try {
            commandApi.update(id, form.getUnitPrice(), form.getCommissionType(), form.getCommissionValue());
            return "redirect:/labels/" + labelId + "/distributors/" + distributorId;
        } catch (IllegalArgumentException e) {
            var label = labelQueryApi.findById(labelId).orElseThrow();
            var distributor = distributorQueryApi.findById(distributorId).orElseThrow();
            model.addAttribute("label", label);
            model.addAttribute("distributor", distributor);
            model.addAttribute("agreement", agreement);
            model.addAttribute("form", form);
            model.addAttribute("productionRunDisplayName", buildDisplayName(agreement.productionRunId()));
            model.addAttribute("commissionTypes", CommissionType.values());
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
        var agreement = queryApi.findById(id)
                .orElseThrow(() -> new AgreementNotFoundException(id));
        if (!agreement.distributorId().equals(distributorId)) {
            throw new AgreementNotFoundException(id);
        }
        commandApi.delete(id);
        return "redirect:/labels/" + labelId + "/distributors/" + distributorId;
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

    private record AvailableProductionRunView(Long productionRunId, String displayName) {
    }
}
