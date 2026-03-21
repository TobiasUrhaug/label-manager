package org.omt.labelmanager.inventory.productionrun.application;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.inventory.InsufficientInventoryException;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;

@ExtendWith(MockitoExtension.class)
class ProductionRunQueryApiImplTest {

    @Mock
    private ProductionRunRepository repository;

    @Mock
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    private ProductionRunQueryApiImpl subject;

    @BeforeEach
    void setUp() {
        subject = new ProductionRunQueryApiImpl(repository, inventoryMovementQueryApi);
    }

    @Test
    void validateQuantityIsAvailable_succeedsWhenRequestedQuantityEqualsAvailable() {
        long runId = 1L;
        ProductionRunEntity entity = productionRunEntityWithQuantity(500);
        when(repository.findById(runId)).thenReturn(Optional.of(entity));
        when(inventoryMovementQueryApi.getWarehouseInventory(runId)).thenReturn(-200);

        assertThatNoException().isThrownBy(() -> subject.validateQuantityIsAvailable(runId, 300));
    }

    @Test
    void validateQuantityIsAvailable_throwsWhenRequestedQuantityExceedsAvailable() {
        long runId = 1L;
        ProductionRunEntity entity = productionRunEntityWithQuantity(500);
        when(repository.findById(runId)).thenReturn(Optional.of(entity));
        when(inventoryMovementQueryApi.getWarehouseInventory(runId)).thenReturn(-200);

        assertThatThrownBy(() -> subject.validateQuantityIsAvailable(runId, 301))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    private ProductionRunEntity productionRunEntityWithQuantity(int quantity) {
        return new ProductionRunEntity(1L, ReleaseFormat.VINYL, null, "Manufacturer", LocalDate.now(), quantity);
    }
}
