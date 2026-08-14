package org.omt.labelmanager.sales.distributorreturn.application;

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
import org.omt.labelmanager.sales.distributorreturn.domain.ReturnLineItemInput;
import org.omt.labelmanager.sales.distributorreturn.infrastructure.DistributorReturnEntity;
import org.omt.labelmanager.sales.distributorreturn.infrastructure.ReturnLineItemEntity;
import org.omt.labelmanager.shared.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Validates a return's line items against what the distributor currently holds, adds them to the
 * return entity, and works out which pressings come back. Shared by {@link RegisterReturnUseCase}
 * and {@link UpdateReturnUseCase} to avoid duplicating inventory and release validation logic.
 */
@Service
class ReturnLineItemProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReturnLineItemProcessor.class);

    private final ReleaseQueryApi releaseQueryApi;
    private final ProductionRunQueryApi productionRunQueryApi;

    ReturnLineItemProcessor(
            ReleaseQueryApi releaseQueryApi, ProductionRunQueryApi productionRunQueryApi) {
        this.releaseQueryApi = releaseQueryApi;
        this.productionRunQueryApi = productionRunQueryApi;
    }

    /**
     * Validates that the distributor holds enough for every line item, adds them to the return
     * entity, and returns the pressings each comes back from.
     *
     * <p>A return draws from the distributor's holding exactly as a sale does — oldest pressing
     * first — so units go back to the warehouse attributed to the pressing they most likely came
     * from. Line items are processed together so two for the same release cannot both be validated
     * against the opening balances.
     *
     * @param lineItems the line items from the form, in order
     * @param labelId the label the return belongs to (for release ownership check)
     * @param from the distributor returning the inventory
     * @param returnEntity the return entity to add the line items to
     * @return every draw the return makes, in line item order — one movement each. Flat rather than
     *     keyed by line item because two line items on one return can be identical, and the caller
     *     needs both.
     */
    List<RunDraw> validateAndAdd(
            List<ReturnLineItemInput> lineItems,
            Long labelId,
            InventoryLocation from,
            DistributorReturnEntity returnEntity) {
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
                                + "before registering returns.");
            }

            List<RunDraw> draws = ledger.drawFifo(lineItemInput.quantity());
            ledgers.put(key, ledger.minus(draws));
            allDraws.addAll(draws);

            returnEntity.addLineItem(
                    new ReturnLineItemEntity(
                            lineItemInput.releaseId(),
                            lineItemInput.format(),
                            lineItemInput.quantity()));

            log.debug(
                    "Processed return line item: release={}, format={}, quantity={}, drawn from {}",
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
