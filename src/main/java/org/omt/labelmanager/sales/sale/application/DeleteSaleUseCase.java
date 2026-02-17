package org.omt.labelmanager.sales.sale.application;

import org.omt.labelmanager.inventory.domain.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.sales.sale.infrastructure.SaleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeleteSaleUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeleteSaleUseCase.class);

    private final SaleRepository saleRepository;
    private final InventoryMovementCommandApi inventoryMovementCommandApi;

    DeleteSaleUseCase(
            SaleRepository saleRepository,
            InventoryMovementCommandApi inventoryMovementCommandApi
    ) {
        this.saleRepository = saleRepository;
        this.inventoryMovementCommandApi = inventoryMovementCommandApi;
    }

    @Transactional
    public void execute(Long saleId) {
        log.info("Deleting sale {}", saleId);

        // Reverse inventory movements before removing the sale
        inventoryMovementCommandApi.deleteMovementsByReference(MovementType.SALE, saleId);

        saleRepository.deleteById(saleId);

        log.info("Sale {} deleted successfully", saleId);
    }
}
