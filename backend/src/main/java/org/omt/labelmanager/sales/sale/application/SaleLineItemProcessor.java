package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.domain.RunDraw;
import org.omt.labelmanager.inventory.domain.StockLedger;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleLineItemEntity;
import org.omt.labelmanager.shared.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validates a sale's line items against business rules, adds them to the sale entity, and works out
 * which pressings each one comes out of. Shared by {@link RegisterSaleUseCase} and {@link
 * UpdateSaleUseCase} to avoid duplicating inventory and release validation logic.
 */
@Service
class SaleLineItemProcessor {

    private static final Logger log = LoggerFactory.getLogger(SaleLineItemProcessor.class);

    private final ReleaseQueryApi releaseQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;

    SaleLineItemProcessor(
            ReleaseQueryApi releaseQueryApi, ProductionRunQueryApi productionRunQueryApi) {
        this.releaseQueryApi = releaseQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
    }

    /**
     * Validates every line item against release ownership and available stock, adds them to the
     * sale entity, and returns the pressings each draws from.
     *
     * <p>All line items are processed together because they share stock: two line items for the
     * same release and format draw from the same pressings, and validating each against the opening
     * balances would let them jointly sell more than exists. Each draw is taken off the ledger the
     * next line item sees.
     *
     * @param lineItems the line items from the form, in order
     * @param labelId the label the sale belongs to (for release ownership check)
     * @param from where the stock is leaving — the counterparty holding it
     * @param saleEntity the sale entity to add the line items to
     * @return every draw the sale makes, in line item order — one movement each. Flat rather than
     *     keyed by line item because two line items on one sale can be identical, and the caller
     *     needs both.
     */
    List<RunDraw> validateAndAdd(
            List<SaleLineItemInput> lineItems,
            Long labelId,
            InventoryLocation from,
            SaleEntity saleEntity) {
        Map<StockKey, StockLedger> ledgers = new HashMap<>();
        List<RunDraw> allDraws = new ArrayList<>();

        for (var lineItemInput : lineItems) {
            var release =
                    releaseQueryApi
                            .findById(lineItemInput.releaseId())
                            .orElseThrow(
                                    () ->
                                            new EntityNotFoundException(
                                                    "Release not found: "
                                                            + lineItemInput.releaseId()));

            if (!release.labelId().equals(labelId)) {
                throw new IllegalArgumentException(
                        "Release "
                                + lineItemInput.releaseId()
                                + " does not belong to label "
                                + labelId);
            }

            var key = new StockKey(lineItemInput.releaseId(), lineItemInput.format());
            var ledger =
                    ledgers.computeIfAbsent(
                            key,
                            k ->
                                    productionRunQueryApi.lockedLedgerAt(
                                            k.releaseId(), k.format(), from));

            if (ledger.runs().isEmpty()) {
                throw new IllegalStateException(
                        "No production run found for release '"
                                + release.name()
                                + "' ("
                                + lineItemInput.format()
                                + "). "
                                + "Please create a production run for this release and format "
                                + "before registering sales.");
            }

            List<RunDraw> draws = ledger.drawFifo(lineItemInput.quantity());
            ledgers.put(key, ledger.minus(draws));
            allDraws.addAll(draws);

            saleEntity.addLineItem(
                    new SaleLineItemEntity(
                            lineItemInput.releaseId(),
                            lineItemInput.format(),
                            lineItemInput.quantity(),
                            lineItemInput.unitPrice().amount(),
                            lineItemInput.unitPrice().currency()));

            log.debug(
                    "Processed line item: release={}, format={}, quantity={}, drawn from {}",
                    lineItemInput.releaseId(),
                    lineItemInput.format(),
                    lineItemInput.quantity(),
                    draws);
        }

        return List.copyOf(allDraws);
    }

    /** Stock is per release and format — a release's vinyl and CD pressings are separate. */
    private record StockKey(Long releaseId, Format format) {}
}
