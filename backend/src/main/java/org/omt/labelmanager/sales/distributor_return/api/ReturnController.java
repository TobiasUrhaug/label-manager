package org.omt.labelmanager.sales.distributor_return.api;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.Distributor;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItem;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels/{labelId}/returns")
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

    record ReturnLineItemRequest(Long releaseId, ReleaseFormat format, int quantity) {
        ReturnLineItemInput toInput() {
            return new ReturnLineItemInput(releaseId, format, quantity);
        }
    }

    record RegisterReturnRequest(
            Long distributorId,
            LocalDate returnDate,
            String notes,
            List<ReturnLineItemRequest> lineItems
    ) {
        List<ReturnLineItemInput> toLineItemInputs() {
            return lineItems.stream().map(ReturnLineItemRequest::toInput).toList();
        }
    }

    record UpdateReturnRequest(LocalDate returnDate, String notes, List<ReturnLineItemRequest> lineItems) {
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
            ReleaseFormat format,
            int quantity
    ) {}

    record ReturnDetailResponse(
            Long id,
            Long labelId,
            Long distributorId,
            LocalDate returnDate,
            String notes,
            Instant createdAt,
            Distributor distributor,
            List<EnrichedReturnLineItem> lineItems
    ) {}

    @GetMapping
    public ReturnListResponse listReturns(@PathVariable Long labelId) {
        labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var returns = returnQueryApi.getReturnsForLabel(labelId);
        var distributors = distributorQueryApi.findByLabelId(labelId);
        return new ReturnListResponse(returns, distributors);
    }

    @PostMapping
    public ResponseEntity<Void> registerReturn(
            @PathVariable Long labelId,
            @RequestBody RegisterReturnRequest request
    ) {
        returnCommandApi.registerReturn(
                labelId,
                request.distributorId(),
                request.returnDate(),
                request.notes(),
                request.toLineItemInputs()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{returnId}")
    public ReturnDetailResponse viewReturn(
            @PathVariable Long labelId,
            @PathVariable Long returnId
    ) {
        labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributorReturn = returnQueryApi.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("Return not found"));
        var distributor = distributorQueryApi.findById(distributorReturn.distributorId())
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
        var lineItems = distributorReturn.lineItems().stream()
                .map(this::enrichLineItem)
                .toList();
        return new ReturnDetailResponse(
                distributorReturn.id(), distributorReturn.labelId(),
                distributorReturn.distributorId(), distributorReturn.returnDate(),
                distributorReturn.notes(), distributorReturn.createdAt(),
                distributor, lineItems
        );
    }

    @PutMapping("/{returnId}")
    public DistributorReturn updateReturn(
            @PathVariable Long labelId,
            @PathVariable Long returnId,
            @RequestBody UpdateReturnRequest request
    ) {
        returnCommandApi.updateReturn(
                returnId,
                request.returnDate(),
                request.notes(),
                request.toLineItemInputs()
        );
        return returnQueryApi.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("Return not found"));
    }

    @DeleteMapping("/{returnId}")
    public ResponseEntity<Void> deleteReturn(
            @PathVariable Long labelId,
            @PathVariable Long returnId
    ) {
        returnCommandApi.deleteReturn(returnId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class,
            InsufficientInventoryException.class})
    public ResponseEntity<Void> handleBadRequest() {
        return ResponseEntity.badRequest().build();
    }

    private EnrichedReturnLineItem enrichLineItem(ReturnLineItem item) {
        var releaseName = releaseQueryApi.findById(item.releaseId())
                .map(r -> r.name())
                .orElse("Unknown");
        return new EnrichedReturnLineItem(
                item.id(), item.returnId(), item.releaseId(), releaseName,
                item.format(), item.quantity()
        );
    }
}
