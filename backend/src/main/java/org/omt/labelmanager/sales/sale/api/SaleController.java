package org.omt.labelmanager.sales.sale.api;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.ChannelType;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItem;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
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
@RequestMapping("/api/labels/{labelId}/sales")
public class SaleController {

    private final SaleCommandApi saleCommandApi;
    private final SaleQueryApi saleQueryApi;
    private final LabelQueryApi labelQueryApi;
    private final ReleaseQueryApi releaseQueryApi;

    public SaleController(
            SaleCommandApi saleCommandApi,
            SaleQueryApi saleQueryApi,
            LabelQueryApi labelQueryApi,
            ReleaseQueryApi releaseQueryApi
    ) {
        this.saleCommandApi = saleCommandApi;
        this.saleQueryApi = saleQueryApi;
        this.labelQueryApi = labelQueryApi;
        this.releaseQueryApi = releaseQueryApi;
    }

    record LineItemRequest(Long releaseId, ReleaseFormat format, int quantity, BigDecimal unitPrice) {
        SaleLineItemInput toInput() {
            return new SaleLineItemInput(releaseId, format, quantity, Money.of(unitPrice));
        }
    }

    record RegisterSaleRequest(
            LocalDate saleDate,
            ChannelType channel,
            Long distributorId,
            String notes,
            List<LineItemRequest> lineItems
    ) {
        List<SaleLineItemInput> toLineItemInputs() {
            return lineItems.stream().map(LineItemRequest::toInput).toList();
        }
    }

    record UpdateSaleRequest(LocalDate saleDate, String notes, List<LineItemRequest> lineItems) {
        List<SaleLineItemInput> toLineItemInputs() {
            return lineItems.stream().map(LineItemRequest::toInput).toList();
        }
    }

    record SaleListResponse(List<Sale> sales, Money totalRevenue) {}

    record EnrichedLineItem(
            Long id,
            Long releaseId,
            String releaseName,
            ReleaseFormat format,
            int quantity,
            Money unitPrice,
            Money lineTotal
    ) {}

    record SaleDetailResponse(
            Long id,
            Long labelId,
            Long distributorId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            Money totalAmount,
            List<EnrichedLineItem> lineItems
    ) {}

    @GetMapping
    public SaleListResponse listSales(@PathVariable Long labelId) {
        labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var sales = saleQueryApi.getSalesForLabel(labelId);
        var totalRevenue = saleQueryApi.getTotalRevenueForLabel(labelId);
        return new SaleListResponse(sales, totalRevenue);
    }

    @PostMapping
    public ResponseEntity<Void> registerSale(
            @PathVariable Long labelId,
            @RequestBody RegisterSaleRequest request
    ) {
        saleCommandApi.registerSale(
                labelId,
                request.saleDate(),
                request.channel(),
                request.notes(),
                request.distributorId(),
                request.toLineItemInputs()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{saleId}")
    public SaleDetailResponse viewSale(
            @PathVariable Long labelId,
            @PathVariable Long saleId
    ) {
        labelQueryApi.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label not found"));
        var sale = saleQueryApi.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        return toDetailResponse(sale);
    }

    @PutMapping("/{saleId}")
    public Sale updateSale(
            @PathVariable Long labelId,
            @PathVariable Long saleId,
            @RequestBody UpdateSaleRequest request
    ) {
        return saleCommandApi.updateSale(
                saleId,
                request.saleDate(),
                request.notes(),
                request.toLineItemInputs()
        );
    }

    @DeleteMapping("/{saleId}")
    public ResponseEntity<Void> deleteSale(
            @PathVariable Long labelId,
            @PathVariable Long saleId
    ) {
        saleCommandApi.deleteSale(saleId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class,
            InsufficientInventoryException.class})
    public ResponseEntity<Void> handleBadRequest() {
        return ResponseEntity.badRequest().build();
    }

    private SaleDetailResponse toDetailResponse(Sale sale) {
        var lineItems = sale.lineItems().stream()
                .map(this::enrichLineItem)
                .toList();
        return new SaleDetailResponse(
                sale.id(), sale.labelId(), sale.distributorId(), sale.saleDate(),
                sale.channel(), sale.notes(), sale.totalAmount(), lineItems
        );
    }

    private EnrichedLineItem enrichLineItem(SaleLineItem item) {
        var releaseName = releaseQueryApi.findById(item.releaseId())
                .map(r -> r.name())
                .orElse("Unknown");
        return new EnrichedLineItem(
                item.id(), item.releaseId(), releaseName,
                item.format(), item.quantity(), item.unitPrice(), item.lineTotal()
        );
    }
}
