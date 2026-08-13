package org.omt.labelmanager.sales.sale.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.catalog.release.domain.Release;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.finance.domain.shared.Money;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;
import org.omt.labelmanager.inventory.productionrun.domain.ProductionRun;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;
import org.omt.labelmanager.sales.sale.infrastructure.SaleEntity;

@ExtendWith(MockitoExtension.class)
class SaleLineItemProcessorTest {

    private static final long LABEL_ID = 1L;
    private static final long RELEASE_ID = 10L;
    private static final long PRODUCTION_RUN_ID = 100L;
    private static final long DISTRIBUTOR_ID = 200L;

    @Mock
    private ReleaseQueryApi releaseQueryApi;

    @Mock
    private ProductionRunQueryApi productionRunQueryApi;

    @Mock
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Mock
    private SaleEntity saleEntity;

    private SaleLineItemProcessor subject;

    @BeforeEach
    void setUp() {
        subject = new SaleLineItemProcessor(
                releaseQueryApi,
                productionRunQueryApi,
                inventoryMovementQueryApi
        );
    }

    @Test
    void validateAndAdd_throwsInsufficientInventoryException_whenDistributorHasZeroStock() {
        var release = releaseWithId(RELEASE_ID);
        var productionRun = productionRunWithId(PRODUCTION_RUN_ID);
        var lineItemInput = lineItemFor(RELEASE_ID, 1);

        when(releaseQueryApi.findById(RELEASE_ID)).thenReturn(Optional.of(release));
        when(productionRunQueryApi.findMostRecent(RELEASE_ID, ReleaseFormat.VINYL))
                .thenReturn(Optional.of(productionRun));
        when(inventoryMovementQueryApi.getCurrentInventory(PRODUCTION_RUN_ID, DISTRIBUTOR_ID))
                .thenReturn(0);

        assertThatThrownBy(() -> subject.validateAndAdd(
                lineItemInput, LABEL_ID, DISTRIBUTOR_ID, saleEntity))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void validateAndAdd_addsLineItemAndReturnsProductionRunId_whenStockIsSufficient() {
        var release = releaseWithId(RELEASE_ID);
        var productionRun = productionRunWithId(PRODUCTION_RUN_ID);
        var lineItemInput = lineItemFor(RELEASE_ID, 5);

        when(releaseQueryApi.findById(RELEASE_ID)).thenReturn(Optional.of(release));
        when(productionRunQueryApi.findMostRecent(RELEASE_ID, ReleaseFormat.VINYL))
                .thenReturn(Optional.of(productionRun));
        when(inventoryMovementQueryApi.getCurrentInventory(PRODUCTION_RUN_ID, DISTRIBUTOR_ID))
                .thenReturn(100);

        Long result = subject.validateAndAdd(lineItemInput, LABEL_ID, DISTRIBUTOR_ID, saleEntity);

        assertThat(result).isEqualTo(PRODUCTION_RUN_ID);
        verify(saleEntity).addLineItem(org.mockito.ArgumentMatchers.any());
    }

    private Release releaseWithId(long releaseId) {
        return new Release(releaseId, "Test Album", LocalDate.now(), LABEL_ID,
                List.of(), List.of(), Set.of(ReleaseFormat.VINYL));
    }

    private ProductionRun productionRunWithId(long productionRunId) {
        return new ProductionRun(productionRunId, RELEASE_ID, ReleaseFormat.VINYL,
                null, "Manufacturer", LocalDate.now(), 500);
    }

    private SaleLineItemInput lineItemFor(long releaseId, int quantity) {
        return new SaleLineItemInput(
                releaseId, ReleaseFormat.VINYL, quantity, new Money(new BigDecimal("15.00"), "EUR"));
    }
}
