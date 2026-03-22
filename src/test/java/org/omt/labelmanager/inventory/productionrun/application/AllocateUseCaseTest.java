package org.omt.labelmanager.inventory.productionrun.application;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.MovementType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementCommandApi;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.persistence.ProductionRunRepository;

@ExtendWith(MockitoExtension.class)
class AllocateUseCaseTest {

    @Mock
    private ProductionRunRepository repository;

    @Mock
    private InventoryMovementQueryApi inventoryMovementQueryApi;

    @Mock
    private InventoryMovementCommandApi inventoryMovementCommandApi;

    private AllocateUseCase subject;

    private static final long RUN_ID = 1L;

    @BeforeEach
    void setUp() {
        subject = new AllocateUseCase(repository, inventoryMovementQueryApi, inventoryMovementCommandApi);
    }

    @Test
    void execute_recordsAllocationMovement_whenQuantityIsAvailable() {
        when(repository.findById(RUN_ID)).thenReturn(Optional.of(runWithQuantity(500)));
        when(inventoryMovementQueryApi.getWarehouseInventory(RUN_ID)).thenReturn(-200);

        assertThatNoException().isThrownBy(() ->
                subject.execute(RUN_ID, InventoryLocation.distributor(5L), 300));

        verify(inventoryMovementCommandApi).recordMovement(
                RUN_ID,
                InventoryLocation.warehouse(),
                InventoryLocation.distributor(5L),
                300,
                MovementType.ALLOCATION,
                null
        );
    }

    @Test
    void execute_throwsInsufficientInventoryException_whenQuantityExceedsAvailable() {
        when(repository.findById(RUN_ID)).thenReturn(Optional.of(runWithQuantity(500)));
        when(inventoryMovementQueryApi.getWarehouseInventory(RUN_ID)).thenReturn(-200);

        assertThatThrownBy(() -> subject.execute(RUN_ID, InventoryLocation.distributor(5L), 301))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void execute_recordsBandcampAllocation_whenLocationIsBandcamp() {
        when(repository.findById(RUN_ID)).thenReturn(Optional.of(runWithQuantity(100)));
        when(inventoryMovementQueryApi.getWarehouseInventory(RUN_ID)).thenReturn(0);

        subject.execute(RUN_ID, InventoryLocation.bandcamp(), 50);

        verify(inventoryMovementCommandApi).recordMovement(
                RUN_ID,
                InventoryLocation.warehouse(),
                InventoryLocation.bandcamp(),
                50,
                MovementType.ALLOCATION,
                null
        );
    }

    private ProductionRunEntity runWithQuantity(int quantity) {
        return new ProductionRunEntity(1L, ReleaseFormat.VINYL, null, "Manufacturer", LocalDate.now(), quantity);
    }
}
