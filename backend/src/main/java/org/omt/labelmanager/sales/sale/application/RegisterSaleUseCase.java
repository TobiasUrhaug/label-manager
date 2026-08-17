package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.distribution.distributor.api.ChannelType;
import org.omt.labelmanager.distribution.distributor.api.Distributor;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.domain.RunDraw;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.sales.sale.domain.Sale;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterSaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterSaleUseCase.class);

    private final SaleRepository saleRepository;
    private final LabelQueryApi labelQueryApi;
    private final DistributorQueryApi distributorQueryApi;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;
    private final SaleLineItemProcessor lineItemProcessor;
    private final SaleConverter saleConverter;

    RegisterSaleUseCase(
            SaleRepository saleRepository,
            LabelQueryApi labelQueryApi,
            DistributorQueryApi distributorQueryApi,
            InventoryMovementCommandApi inventoryMovementCommandApi,
            SaleLineItemProcessor lineItemProcessor,
            SaleConverter saleConverter) {
        this.saleRepository = saleRepository;
        this.labelQueryApi = labelQueryApi;
        this.distributorQueryApi = distributorQueryApi;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
        this.lineItemProcessor = lineItemProcessor;
        this.saleConverter = saleConverter;
    }

    @Transactional
    public Sale execute(
            Long labelId,
            LocalDate saleDate,
            ChannelType channel,
            String notes,
            Long distributorId,
            List<SaleLineItemInput> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Sale must contain at least one line item");
        }

        log.info("Registering sale for label {} with {} line items", labelId, lineItems.size());

        // 1. Validate label exists
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }

        // 2. Determine which distributor to use (fetched once; passed down to avoid re-querying)
        Distributor distributor = determineDistributor(labelId, channel, distributorId);

        // 3. Create sale entity
        var saleEntity =
                new SaleEntity(
                        labelId,
                        distributor.id(),
                        saleDate,
                        channel,
                        notes,
                        lineItems.getFirst().unitPrice().currency());

        // 4. Validate the line items and work out which pressings each draws from
        InventoryLocation from = InventoryLocation.distributor(distributor.id());
        List<RunDraw> draws =
                lineItemProcessor.validateAndAdd(lineItems, labelId, from, saleEntity);

        // 5. Save sale
        var savedSale = saleRepository.save(saleEntity);

        // 6. Record SALE movements (after save so saleId is available as referenceId). One per
        // pressing drawn from — a line item that spans two pressings records two.
        for (RunDraw draw : draws) {
            inventoryMovementCommandApi.recordMovement(
                    draw.productionRunId(),
                    from,
                    InventoryLocation.external(),
                    draw.quantity(),
                    MovementType.SALE,
                    savedSale.getId());
        }

        log.info(
                "Sale registered successfully with ID {} and total amount {}",
                savedSale.getId(),
                savedSale.getTotalAmount());

        return saleConverter.toSale(savedSale);
    }

    private Distributor determineDistributor(
            Long labelId, ChannelType channel, Long distributorId) {
        if (channel == ChannelType.DIRECT) {
            return distributorQueryApi
                    .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                    .orElseThrow(
                            () ->
                                    new EntityNotFoundException(
                                            "DIRECT distributor not found for label: " + labelId));
        }

        if (distributorId == null) {
            throw new IllegalArgumentException(
                    "Distributor must be specified for " + channel + " sales");
        }

        var distributor =
                distributorQueryApi
                        .findById(distributorId)
                        .filter(d -> d.labelId().equals(labelId))
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Distributor "
                                                        + distributorId
                                                        + " not found for label "
                                                        + labelId));

        if (distributor.channelType() != channel) {
            throw new IllegalArgumentException(
                    "Distributor '"
                            + distributor.name()
                            + "' (type: "
                            + distributor.channelType()
                            + ") does not match channel type: "
                            + channel);
        }

        return distributor;
    }
}
