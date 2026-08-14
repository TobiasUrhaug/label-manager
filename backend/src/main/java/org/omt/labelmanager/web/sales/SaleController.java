package org.omt.labelmanager.web.sales;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.sale.api.SaleCommandApi;
import org.omt.labelmanager.sales.sale.api.SaleQueryApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.shared.Format;
import org.omt.labelmanager.shared.Money;
import org.omt.labelmanager.web.LabelScope;
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
public class SaleController {

    private final SaleCommandApi saleCommandApi;
    private final SaleQueryApi saleQueryApi;
    private final LabelScope labelScope;
    private final ReleaseQueryApi releaseQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;
    private final DistributorQueryApi distributorQueryApi;

    public SaleController(
            SaleCommandApi saleCommandApi,
            SaleQueryApi saleQueryApi,
            LabelScope labelScope,
            ReleaseQueryApi releaseQueryApi,
            ProductionRunQueryApi productionRunQueryApi,
            DistributorQueryApi distributorQueryApi) {
        this.saleCommandApi = saleCommandApi;
        this.saleQueryApi = saleQueryApi;
        this.labelScope = labelScope;
        this.releaseQueryApi = releaseQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
        this.distributorQueryApi = distributorQueryApi;
    }

    record LineItemRequest(Long releaseId, Format format, int quantity, BigDecimal unitPrice) {
        SaleLineItemInput toInput() {
            return new SaleLineItemInput(releaseId, format, quantity, Money.of(unitPrice));
        }
    }

    record RegisterSaleRequest(
            @NotNull LocalDate saleDate,
            @NotNull ChannelType channel,
            Long distributorId,
            String notes,
            @NotEmpty List<LineItemRequest> lineItems) {
        List<SaleLineItemInput> toLineItemInputs() {
            return lineItems.stream().map(LineItemRequest::toInput).toList();
        }
    }

    record UpdateSaleRequest(
            @NotNull LocalDate saleDate, String notes, @NotEmpty List<LineItemRequest> lineItems) {
        List<SaleLineItemInput> toLineItemInputs() {
            return lineItems.stream().map(LineItemRequest::toInput).toList();
        }
    }

    record SaleListResponse(List<Sale> sales, Money totalRevenue) {}

    record EnrichedLineItem(
            Long id,
            Long releaseId,
            String releaseName,
            Format format,
            int quantity,
            Money unitPrice,
            Money lineTotal) {}

    record SaleDetailResponse(
            Long id,
            Long labelId,
            Long distributorId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            Money totalAmount,
            List<EnrichedLineItem> lineItems) {}

    record ReleaseSaleView(
            Long saleId,
            LocalDate saleDate,
            String distributorName,
            int totalUnits,
            Money totalRevenue) {}

    record ReleaseSalesResponse(List<ReleaseSaleView> sales, int totalUnitsSold) {}

    /**
     * The sales attributed to one release, through its production runs.
     *
     * <p>Replaces the {@code releaseSales} and {@code totalUnitsSold} fields of the release detail
     * response.
     */
    @GetMapping("/api/labels/{labelId}/releases/{releaseId}/sales")
    public ReleaseSalesResponse salesForRelease(
            @PathVariable Long labelId, @PathVariable Long releaseId) {
        labelScope.requireRelease(labelId, releaseId);

        List<Distributor> distributors = distributorQueryApi.findByLabelId(labelId);
        List<ReleaseSaleView> sales =
                productionRunQueryApi.findByReleaseId(releaseId).stream()
                        .flatMap(run -> saleQueryApi.getSalesForProductionRun(run.id()).stream())
                        .map(sale -> toReleaseSaleView(sale, distributors))
                        .sorted(Comparator.comparing(ReleaseSaleView::saleDate).reversed())
                        .toList();

        int totalUnitsSold = sales.stream().mapToInt(ReleaseSaleView::totalUnits).sum();
        return new ReleaseSalesResponse(sales, totalUnitsSold);
    }

    private ReleaseSaleView toReleaseSaleView(Sale sale, List<Distributor> distributors) {
        int totalUnits = sale.lineItems().stream().mapToInt(SaleLineItem::quantity).sum();
        String distributorName =
                distributors.stream()
                        .filter(d -> d.id().equals(sale.distributorId()))
                        .findFirst()
                        .map(Distributor::name)
                        .orElse("Unknown");
        return new ReleaseSaleView(
                sale.id(), sale.saleDate(), distributorName, totalUnits, sale.totalAmount());
    }

    /**
     * The sales made through one distributor. Replaces the {@code sales} field of the distributor
     * detail response.
     */
    @GetMapping("/api/labels/{labelId}/distributors/{distributorId}/sales")
    public List<Sale> salesForDistributor(
            @PathVariable Long labelId, @PathVariable Long distributorId) {
        labelScope.requireDistributor(labelId, distributorId);
        return saleQueryApi.getSalesForDistributor(distributorId);
    }

    @GetMapping("/api/labels/{labelId}/sales")
    public SaleListResponse listSales(@PathVariable Long labelId) {
        labelScope.requireLabel(labelId);
        var sales = saleQueryApi.getSalesForLabel(labelId);
        var totalRevenue = saleQueryApi.getTotalRevenueForLabel(labelId);
        return new SaleListResponse(sales, totalRevenue);
    }

    @PostMapping("/api/labels/{labelId}/sales")
    public ResponseEntity<Void> registerSale(
            @PathVariable Long labelId, @Valid @RequestBody RegisterSaleRequest request) {
        if (request.distributorId() == null) {
            labelScope.requireLabel(labelId);
        } else {
            labelScope.requireDistributor(labelId, request.distributorId());
        }
        saleCommandApi.registerSale(
                labelId,
                request.saleDate(),
                request.channel(),
                request.notes(),
                request.distributorId(),
                request.toLineItemInputs());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/labels/{labelId}/sales/{saleId}")
    public SaleDetailResponse viewSale(@PathVariable Long labelId, @PathVariable Long saleId) {
        return toDetailResponse(requireSaleOfLabel(labelId, saleId));
    }

    @PutMapping("/api/labels/{labelId}/sales/{saleId}")
    public Sale updateSale(
            @PathVariable Long labelId,
            @PathVariable Long saleId,
            @Valid @RequestBody UpdateSaleRequest request) {
        requireSaleOfLabel(labelId, saleId);
        return saleCommandApi.updateSale(
                saleId, request.saleDate(), request.notes(), request.toLineItemInputs());
    }

    @DeleteMapping("/api/labels/{labelId}/sales/{saleId}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long labelId, @PathVariable Long saleId) {
        requireSaleOfLabel(labelId, saleId);
        saleCommandApi.deleteSale(saleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * SaleCommandApi's update and delete take a sale id and nothing else, so the label in the path
     * is only meaningful if it is checked here.
     */
    private Sale requireSaleOfLabel(Long labelId, Long saleId) {
        return saleQueryApi
                .findById(saleId)
                .filter(sale -> labelId.equals(sale.labelId()))
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Sale " + saleId + " does not belong to label " + labelId));
    }

    private SaleDetailResponse toDetailResponse(Sale sale) {
        var lineItems = sale.lineItems().stream().map(this::enrichLineItem).toList();
        return new SaleDetailResponse(
                sale.id(),
                sale.labelId(),
                sale.distributorId(),
                sale.saleDate(),
                sale.channel(),
                sale.notes(),
                sale.totalAmount(),
                lineItems);
    }

    private EnrichedLineItem enrichLineItem(SaleLineItem item) {
        var releaseName =
                releaseQueryApi.findById(item.releaseId()).map(r -> r.name()).orElse("Unknown");
        return new EnrichedLineItem(
                item.id(),
                item.releaseId(),
                releaseName,
                item.format(),
                item.quantity(),
                item.unitPrice(),
                item.lineTotal());
    }
}
