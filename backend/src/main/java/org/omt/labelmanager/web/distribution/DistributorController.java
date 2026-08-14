package org.omt.labelmanager.web.distribution;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.agreement.api.PricingAgreement;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorCommandApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
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
    private final ProductionRunQueryApi productionRunQueryApi;
    private final ReleaseQueryApi releaseQueryApi;

    public DistributorController(
            DistributorCommandApi commandApi,
            DistributorQueryApi distributorQueryApi,
            LabelQueryApi labelQueryApi,
            AgreementQueryApi agreementQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            ReleaseQueryApi releaseQueryApi) {
        this.commandApi = commandApi;
        this.distributorQueryApi = distributorQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.agreementQueryApi = agreementQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.releaseQueryApi = releaseQueryApi;
    }

    record AddDistributorRequest(String name, ChannelType channelType) {}

    /**
     * The label's distributors. Replaces the list that {@code GET /api/labels/{labelId}} bundled.
     */
    @GetMapping
    public List<Distributor> distributors(@PathVariable Long labelId) {
        labelQueryApi
                .findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
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
        return requireDistributorOfLabel(labelId, distributorId);
    }

    /** The distributor's agreements, each naming the production run it prices. */
    @GetMapping("/{distributorId}/agreements")
    public List<AgreementView> agreements(
            @PathVariable Long labelId, @PathVariable Long distributorId) {
        requireDistributorOfLabel(labelId, distributorId);
        return agreementQueryApi.findByDistributorId(distributorId).stream()
                .map(this::enrichAgreement)
                .toList();
    }

    private Distributor requireDistributorOfLabel(Long labelId, Long distributorId) {
        labelQueryApi
                .findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        return distributorQueryApi
                .findById(distributorId)
                .filter(d -> d.labelId().equals(labelId))
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
    }

    @PostMapping
    public ResponseEntity<Void> addDistributor(
            @PathVariable Long labelId, @RequestBody AddDistributorRequest request) {
        commandApi.createDistributor(labelId, request.name(), request.channelType());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{distributorId}")
    public ResponseEntity<Void> deleteDistributor(
            @PathVariable Long labelId, @PathVariable Long distributorId) {
        requireDistributorOfLabel(labelId, distributorId);
        commandApi.delete(distributorId);
        return ResponseEntity.noContent().build();
    }

    private AgreementView enrichAgreement(PricingAgreement agreement) {
        var displayName =
                productionRunQueryApi
                        .findById(agreement.productionRunId())
                        .map(
                                run -> {
                                    var title =
                                            releaseQueryApi
                                                    .findById(run.releaseId())
                                                    .map(r -> r.name())
                                                    .orElse("Unknown Release");
                                    return title + " – " + run.format();
                                })
                        .orElse("Unknown");
        return new AgreementView(agreement, displayName);
    }
}
