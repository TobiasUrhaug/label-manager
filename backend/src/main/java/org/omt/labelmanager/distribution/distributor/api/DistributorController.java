package org.omt.labelmanager.distribution.distributor.api;

import jakarta.persistence.EntityNotFoundException;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.agreement.PricingAgreement;
import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.omt.labelmanager.distribution.distributor.Distributor;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.distributor_return.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.distributor_return.domain.DistributorReturn;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/labels/{labelId}/distributors")
public class DistributorController {

    private final DistributorCommandApi commandApi;
    private final DistributorQueryApi distributorQueryApi;
    private final LabelQueryApi labelQueryApi;
    private final SaleQueryApi saleQueryApi;
    private final DistributorReturnQueryApi returnQueryApi;
    private final AgreementQueryApi agreementQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final ReleaseQueryApi releaseQueryApi;

    public DistributorController(
            DistributorCommandApi commandApi,
            DistributorQueryApi distributorQueryApi,
            LabelQueryApi labelQueryApi,
            SaleQueryApi saleQueryApi,
            DistributorReturnQueryApi returnQueryApi,
            AgreementQueryApi agreementQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            ReleaseQueryApi releaseQueryApi
    ) {
        this.commandApi = commandApi;
        this.distributorQueryApi = distributorQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.saleQueryApi = saleQueryApi;
        this.returnQueryApi = returnQueryApi;
        this.agreementQueryApi = agreementQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.releaseQueryApi = releaseQueryApi;
    }

    record AddDistributorRequest(String name, ChannelType channelType) {}

    record DistributorDetailResponse(
            Distributor distributor,
            List<Sale> sales,
            List<DistributorReturn> returns,
            List<AgreementView> agreements
    ) {}

    @GetMapping("/{distributorId}")
    public DistributorDetailResponse showDistributor(
            @PathVariable Long labelId,
            @PathVariable Long distributorId
    ) {
        labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var distributor = distributorQueryApi.findById(distributorId)
                .filter(d -> d.labelId().equals(labelId))
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
        var sales = saleQueryApi.getSalesForDistributor(distributorId);
        var returns = returnQueryApi.getReturnsForDistributor(distributorId);
        var agreements = agreementQueryApi.findByDistributorId(distributorId).stream()
                .map(this::enrichAgreement)
                .toList();

        return new DistributorDetailResponse(distributor, sales, returns, agreements);
    }

    @PostMapping
    public ResponseEntity<Void> addDistributor(
            @PathVariable Long labelId,
            @RequestBody AddDistributorRequest request
    ) {
        commandApi.createDistributor(labelId, request.name(), request.channelType());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{distributorId}")
    public ResponseEntity<Void> deleteDistributor(
            @PathVariable Long labelId,
            @PathVariable Long distributorId
    ) {
        commandApi.delete(distributorId);
        return ResponseEntity.noContent().build();
    }

    private AgreementView enrichAgreement(PricingAgreement agreement) {
        var displayName = productionRunQueryApi.findById(agreement.productionRunId())
                .map(run -> {
                    var title = releaseQueryApi.findById(run.releaseId())
                            .map(r -> r.name())
                            .orElse("Unknown Release");
                    return title + " \u2013 " + run.format();
                })
                .orElse("Unknown");
        return new AgreementView(agreement, displayName);
    }
}
