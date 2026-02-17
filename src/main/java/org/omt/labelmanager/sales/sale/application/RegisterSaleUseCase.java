package org.omt.labelmanager.sales.sale.application;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.omt.labelmanager.catalog.label.api.LabelQueryApi;
import org.omt.labelmanager.distribution.distributor.api.DistributorQueryApi;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.domain.Distributor;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.domain.MovementType;
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
            SaleConverter saleConverter
    ) {
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
            List<SaleLineItemInput> lineItems
    ) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalArgumentException("Sale must contain at least one line item");
        }

        log.info("Registering sale for label {} with {} line items",
                labelId, lineItems.size());

        // 1. Validate label exists
        if (!labelQueryApi.exists(labelId)) {
            throw new EntityNotFoundException("Label not found: " + labelId);
        }

        // 2. Determine which distributor to use (fetched once; passed down to avoid re-querying)
        Distributor distributor = determineDistributor(labelId, channel, distributorId);

        // 3. Create sale entity
        var saleEntity = new SaleEntity(
                labelId,
                distributor.id(),
                saleDate,
                channel,
                notes,
                lineItems.getFirst().unitPrice().currency()
        );

        // 4. Validate each line item and cache its production run ID for step 6
        Map<SaleLineItemInput, Long> productionRunIds = new LinkedHashMap<>();
        for (var lineItemInput : lineItems) {
            Long productionRunId = lineItemProcessor.validateAndAdd(
                    lineItemInput, labelId, distributor.id(), distributor.name(), saleEntity
            );
            productionRunIds.put(lineItemInput, productionRunId);
        }

        // 5. Save sale
        var savedSale = saleRepository.save(saleEntity);

        // 6. Record SALE movements (after save so saleId is available as referenceId)
        for (var entry : productionRunIds.entrySet()) {
            inventoryMovementCommandApi.recordMovement(
                    entry.getValue(),
                    LocationType.DISTRIBUTOR,
                    distributor.id(),
                    LocationType.EXTERNAL,
                    null,
                    entry.getKey().quantity(),
                    MovementType.SALE,
                    savedSale.getId()
            );
        }

        log.info("Sale registered successfully with ID {} and total amount {}",
                savedSale.getId(), savedSale.getTotalAmount());

        return saleConverter.toSale(savedSale);
    }

    private Distributor determineDistributor(
            Long labelId,
            ChannelType channel,
            Long distributorId
    ) {
        if (channel == ChannelType.DIRECT) {
            return distributorQueryApi
                    .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "DIRECT distributor not found for label: " + labelId
                    ));
        }

        if (distributorId == null) {
            throw new IllegalArgumentException(
                    "Distributor must be specified for " + channel + " sales"
            );
        }

        var distributor = distributorQueryApi
                .findByLabelId(labelId)
                .stream()
                .filter(d -> d.id().equals(distributorId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Distributor not found: " + distributorId
                ));

        if (distributor.channelType() != channel) {
            throw new IllegalArgumentException(
                    "Distributor '" + distributor.name()
                            + "' (type: " + distributor.channelType()
                            + ") does not match channel type: " + channel
            );
        }

        return distributor;
    }
}
