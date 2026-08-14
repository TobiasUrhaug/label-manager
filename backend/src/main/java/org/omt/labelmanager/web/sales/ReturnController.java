package org.omt.labelmanager.web.sales;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.sales.distributorreturn.api.DistributorReturnCommandApi;
import org.omt.labelmanager.sales.distributorreturn.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.distributorreturn.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributorreturn.domain.ReturnLineItem;
import org.omt.labelmanager.sales.distributorreturn.domain.ReturnLineItemInput;
import org.omt.labelmanager.shared.Format;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
            DistributorQueryApi distributorQueryApi) {
        this.returnCommandApi = returnCommandApi;
        this.returnQueryApi = returnQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
        this.distributorQueryApi = distributorQueryApi;
    }

    private void requireLabel(Long labelId) {
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }
    }

    private void requireDistributor(Long labelId, Long distributorId) {
        if (!distributorQueryApi.belongsToLabel(distributorId, labelId)) {
            throw new EntityNotFoundException(
                    "Distributor " + distributorId + " does not belong to label " + labelId);
        }
    }

    record ReturnLineItemRequest(Long releaseId, Format format, int quantity) {
        ReturnLineItemInput toInput() {
            return new ReturnLineItemInput(releaseId, format, quantity);
        }
    }

    record RegisterReturnRequest(
            @NotNull Long distributorId,
            @NotNull LocalDate returnDate,
            String notes,
            @NotEmpty List<ReturnLineItemRequest> lineItems) {
        List<ReturnLineItemInput> toLineItemInputs() {
            return lineItems.stream().map(ReturnLineItemRequest::toInput).toList();
        }
    }

    record UpdateReturnRequest(
            @NotNull LocalDate returnDate,
            String notes,
            @NotEmpty List<ReturnLineItemRequest> lineItems) {
        List<ReturnLineItemInput> toLineItemInputs() {
            return lineItems.stream().map(ReturnLineItemRequest::toInput).toList();
        }
    }

    record ReturnListResponse(List<DistributorReturn> returns, List<Distributor> distributors) {}

    record EnrichedReturnLineItem(
            Long id,
            Long returnId,
            Long releaseId,
            String releaseName,
            Format format,
            int quantity) {}

    record ReturnDetailResponse(
            Long id,
            Long labelId,
            Long distributorId,
            LocalDate returnDate,
            String notes,
            Instant createdAt,
            Distributor distributor,
            List<EnrichedReturnLineItem> lineItems) {}

    /**
     * The returns received from one distributor. Replaces the {@code returns} field of the
     * distributor detail response.
     */
    @GetMapping("/api/labels/{labelId}/distributors/{distributorId}/returns")
    public List<DistributorReturn> returnsForDistributor(
            @PathVariable Long labelId, @PathVariable Long distributorId) {
        requireDistributor(labelId, distributorId);
        return returnQueryApi.getReturnsForDistributor(distributorId);
    }

    @GetMapping("/api/labels/{labelId}/returns")
    public ReturnListResponse listReturns(@PathVariable Long labelId) {
        requireLabel(labelId);
        var returns = returnQueryApi.getReturnsForLabel(labelId);
        var distributors = distributorQueryApi.findByLabelId(labelId);
        return new ReturnListResponse(returns, distributors);
    }

    @PostMapping("/api/labels/{labelId}/returns")
    public ResponseEntity<Void> registerReturn(
            @PathVariable Long labelId, @Valid @RequestBody RegisterReturnRequest request) {
        requireDistributor(labelId, request.distributorId());
        returnCommandApi.registerReturn(
                labelId,
                request.distributorId(),
                request.returnDate(),
                request.notes(),
                request.toLineItemInputs());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/labels/{labelId}/returns/{returnId}")
    public ReturnDetailResponse viewReturn(
            @PathVariable Long labelId, @PathVariable Long returnId) {
        var distributorReturn = requireReturnOfLabel(labelId, returnId);
        var distributor =
                distributorQueryApi
                        .findById(distributorReturn.distributorId())
                        .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
        var lineItems = distributorReturn.lineItems().stream().map(this::enrichLineItem).toList();
        return new ReturnDetailResponse(
                distributorReturn.id(),
                distributorReturn.labelId(),
                distributorReturn.distributorId(),
                distributorReturn.returnDate(),
                distributorReturn.notes(),
                distributorReturn.createdAt(),
                distributor,
                lineItems);
    }

    @PutMapping("/api/labels/{labelId}/returns/{returnId}")
    public DistributorReturn updateReturn(
            @PathVariable Long labelId,
            @PathVariable Long returnId,
            @Valid @RequestBody UpdateReturnRequest request) {
        requireReturnOfLabel(labelId, returnId);
        returnCommandApi.updateReturn(
                returnId, request.returnDate(), request.notes(), request.toLineItemInputs());
        return returnQueryApi
                .findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("Return not found"));
    }

    @DeleteMapping("/api/labels/{labelId}/returns/{returnId}")
    public ResponseEntity<Void> deleteReturn(
            @PathVariable Long labelId, @PathVariable Long returnId) {
        requireReturnOfLabel(labelId, returnId);
        returnCommandApi.deleteReturn(returnId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DistributorReturnCommandApi's update and delete take a return id and nothing else, so the
     * label in the path is only meaningful if it is checked here.
     */
    private DistributorReturn requireReturnOfLabel(Long labelId, Long returnId) {
        return returnQueryApi
                .findById(returnId)
                .filter(distributorReturn -> labelId.equals(distributorReturn.labelId()))
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Return "
                                                + returnId
                                                + " does not belong to label "
                                                + labelId));
    }

    private EnrichedReturnLineItem enrichLineItem(ReturnLineItem item) {
        var releaseName =
                releaseQueryApi.findById(item.releaseId()).map(r -> r.name()).orElse("Unknown");
        return new EnrichedReturnLineItem(
                item.id(),
                item.returnId(),
                item.releaseId(),
                releaseName,
                item.format(),
                item.quantity());
    }
}
