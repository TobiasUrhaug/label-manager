package org.omt.labelmanager.distribution.distributor.web;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.agreement.api.PricingAgreement;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels/{labelId}/distributors")
public class DistributorController {

    private final DistributorCommandApi commandApi;
    private final DistributorQueryApi distributorQueryApi;
    private final LabelQueryApi labelQueryApi;
    private final AgreementQueryApi agreementQueryApi;

    public DistributorController(
            DistributorCommandApi commandApi,
            DistributorQueryApi distributorQueryApi,
            LabelQueryApi labelQueryApi,
            AgreementQueryApi agreementQueryApi) {
        this.commandApi = commandApi;
        this.distributorQueryApi = distributorQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.agreementQueryApi = agreementQueryApi;
    }

    private void requireLabel(Long labelId) {
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }
    }

    record AddDistributorRequest(String name, ChannelType channelType) {}

    /**
     * The label's distributors. Replaces the list that {@code GET /api/labels/{labelId}} bundled.
     */
    @GetMapping
    public List<Distributor> distributors(@PathVariable Long labelId) {
        requireLabel(labelId);
        return distributorQueryApi.findByLabelId(labelId);
    }

    /**
     * The distributor itself.
     *
     * <p>Its sales, returns and agreements are separate collections — see {@code
     * /api/labels/{labelId}/sales?distributorId=}, {@code
     * /api/labels/{labelId}/returns?distributorId=} and {@code .../distributors/{id}/agreements}.
     * Bundling them made distribution depend on sales.
     */
    @GetMapping("/{distributorId}")
    public Distributor showDistributor(
            @PathVariable Long labelId, @PathVariable Long distributorId) {
        return requireDistributorOfLabel(distributorId, labelId);
    }

    /**
     * The distributor's agreements.
     *
     * <p>Each carries {@code productionRunId}, not a rendered "Release – FORMAT" label. Resolving
     * that here meant distribution reading from inventory and then catalog, sideways and only for
     * display; the caller joins against {@code /api/labels/{labelId}/production-runs}.
     */
    @GetMapping("/{distributorId}/agreements")
    public List<PricingAgreement> agreements(
            @PathVariable Long labelId, @PathVariable Long distributorId) {
        requireDistributorOfLabel(distributorId, labelId);
        return agreementQueryApi.findByDistributorId(distributorId);
    }

    /** Same (childId, labelId) order as DistributorQueryApi.belongsToLabel, deliberately. */
    private Distributor requireDistributorOfLabel(Long distributorId, Long labelId) {
        return distributorQueryApi
                .findById(distributorId)
                .filter(distributor -> labelId.equals(distributor.labelId()))
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Distributor "
                                                + distributorId
                                                + " does not belong to label "
                                                + labelId));
    }

    @PostMapping
    public ResponseEntity<Void> addDistributor(
            @PathVariable Long labelId, @RequestBody AddDistributorRequest request) {
        // DistributorCommandService saves without validating labelId, so without this a
        // nonexistent label id returns 201 and writes an orphan row.
        requireLabel(labelId);
        commandApi.createDistributor(labelId, request.name(), request.channelType());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{distributorId}")
    public ResponseEntity<Void> deleteDistributor(
            @PathVariable Long labelId, @PathVariable Long distributorId) {
        requireDistributorOfLabel(distributorId, labelId);
        commandApi.delete(distributorId);
        return ResponseEntity.noContent().build();
    }
}
