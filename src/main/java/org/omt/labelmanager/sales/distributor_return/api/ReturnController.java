package org.omt.labelmanager.sales.distributor_return.api;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/labels/{labelId}/returns")
public class ReturnController {

    private final DistributorReturnCommandApi returnCommandApi;
    private final DistributorReturnQueryApi returnQueryApi;
    private final LabelQueryApi labelQueryApi;
    private final ReleaseQueryApi releaseQueryApi;
    private final DistributorQueryApi distributorQueryApi;

    public ReturnController(
            DistributorReturnCommandApi returnCommandApi,
            DistributorReturnQueryApi returnQueryApi,
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi,
            DistributorQueryApi distributorQueryApi
    ) {
        this.returnCommandApi = returnCommandApi;
        this.returnQueryApi = returnQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
        this.distributorQueryApi = distributorQueryApi;
    }

    @GetMapping
    public String listReturns(@PathVariable Long labelId, Model model) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var returns = returnQueryApi.getReturnsForLabel(labelId);
        var distributors = distributorQueryApi.findByLabelId(labelId);
        var distributorNames = toNameMap(distributors);

        model.addAttribute("label", label);
        model.addAttribute("returns", returns);
        model.addAttribute("distributors", distributors);
        model.addAttribute("distributorNames", distributorNames);

        return "return/list";
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
        model.addAttribute("form", new RegisterReturnForm());

        return "return/register";
    }

    @PostMapping
    public String registerReturn(
            @PathVariable Long labelId,
            RegisterReturnForm form,
            Model model
    ) {
        try {
            returnCommandApi.registerReturn(
                    labelId,
                    form.getDistributorId(),
                    form.getReturnDate(),
                    form.getNotes(),
                    form.toLineItemInputs()
            );

            return "redirect:/labels/" + labelId + "/returns";
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
            model.addAttribute("form", form);
            model.addAttribute("errorMessage", e.getMessage());

            return "return/register";
        }
    }

    @GetMapping("/{returnId}")
    public String viewReturn(
            @PathVariable Long labelId,
            @PathVariable Long returnId,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributorReturn = returnQueryApi.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("Return not found"));

        var releaseNames = distributorReturn.lineItems().stream()
                .map(item -> releaseQueryApi.findById(item.releaseId())
                        .map(r -> r.name())
                        .orElse("Unknown"))
                .toList();

        var distributor = distributorQueryApi.findByLabelId(labelId)
                .stream()
                .filter(d -> d.id().equals(distributorReturn.distributorId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        model.addAttribute("label", label);
        model.addAttribute("distributorReturn", distributorReturn);
        model.addAttribute("distributor", distributor);
        model.addAttribute("releaseNames", releaseNames);

        return "return/detail";
    }

    @GetMapping("/{returnId}/edit")
    public String showEditForm(
            @PathVariable Long labelId,
            @PathVariable Long returnId,
            Model model
    ) {
        var label = labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributorReturn = returnQueryApi.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("Return not found"));
        var releases = releaseQueryApi.getReleasesForLabel(labelId);
        var distributors = distributorQueryApi.findByLabelId(labelId);
        var distributorNames = toNameMap(distributors);

        model.addAttribute("label", label);
        model.addAttribute("distributorReturn", distributorReturn);
        model.addAttribute("releases", releases);
        model.addAttribute("distributors", distributors);
        model.addAttribute("distributorNames", distributorNames);
        model.addAttribute("formats", ReleaseFormat.values());
        model.addAttribute("form", buildEditForm(distributorReturn));

        return "return/edit";
    }

    @PostMapping("/{returnId}")
    public String submitEdit(
            @PathVariable Long labelId,
            @PathVariable Long returnId,
            EditReturnForm form,
            Model model
    ) {
        try {
            returnCommandApi.updateReturn(
                    returnId,
                    form.getReturnDate(),
                    form.getNotes(),
                    form.toLineItemInputs()
            );
            return "redirect:/labels/" + labelId + "/returns/" + returnId;
        } catch (IllegalStateException | IllegalArgumentException
                | InsufficientInventoryException e) {
            var label = labelQueryApi.findById(labelId)
                    .orElseThrow(() -> new EntityNotFoundException("Label not found"));
            var distributorReturn = returnQueryApi.findById(returnId)
                    .orElseThrow(() -> new EntityNotFoundException("Return not found"));
            var releases = releaseQueryApi.getReleasesForLabel(labelId);
            var distributors = distributorQueryApi.findByLabelId(labelId);
            var distributorNames = toNameMap(distributors);

            model.addAttribute("label", label);
            model.addAttribute("distributorReturn", distributorReturn);
            model.addAttribute("releases", releases);
            model.addAttribute("distributors", distributors);
            model.addAttribute("distributorNames", distributorNames);
            model.addAttribute("formats", ReleaseFormat.values());
            model.addAttribute("form", form);
            model.addAttribute("errorMessage", e.getMessage());

            return "return/edit";
        }
    }

    @PostMapping("/{returnId}/delete")
    public String deleteReturn(
            @PathVariable Long labelId,
            @PathVariable Long returnId
    ) {
        try {
            returnCommandApi.deleteReturn(returnId);
        } catch (EntityNotFoundException ignored) {
            // Return already gone — redirect gracefully
        }
        return "redirect:/labels/" + labelId + "/returns";
    }

    private Map<Long, String> toNameMap(List<Distributor> distributors) {
        return distributors.stream()
                .collect(Collectors.toMap(Distributor::id, Distributor::name));
    }

    private EditReturnForm buildEditForm(DistributorReturn distributorReturn) {
        var form = new EditReturnForm();
        form.setReturnDate(distributorReturn.returnDate());
        form.setNotes(distributorReturn.notes());
        form.setLineItems(
                distributorReturn.lineItems().stream()
                        .map(item -> new ReturnLineItemForm(
                                item.releaseId(),
                                item.format(),
                                item.quantity()
                        ))
                        .collect(Collectors.toList())
        );
        return form;
    }
}
