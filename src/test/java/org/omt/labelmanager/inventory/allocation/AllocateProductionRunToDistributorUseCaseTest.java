package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.inventory.allocation.application.CreateAllocationUseCase;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.domain.InsufficientInventoryException;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.productionrun.api.ProductionRunQueryApi;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllocateProductionRunToDistributorUseCaseTest {

    @Mock
    private AllocationCommandApi allocationCommandApi;

    @Mock
    private ProductionRunQueryApi productionRunQueryApi;

    @InjectMocks
    private CreateAllocationUseCase useCase;

    private static final Long PRODUCTION_RUN_ID = 1L;
    private static final Long DISTRIBUTOR_ID = 2L;
    private static final int QUANTITY = 100;

    private ChannelAllocation expectedAllocation;

    @BeforeEach
    void setUp() {
        expectedAllocation = new ChannelAllocation(
                10L,
                PRODUCTION_RUN_ID,
                DISTRIBUTOR_ID,
                QUANTITY,
                0,
                Instant.now()
        );
    }

    @Test
    void throwsException_whenProductionRunCannotAllocate() {
        doThrow(new InsufficientInventoryException(200, 50))
                .when(productionRunQueryApi)
                .validateQuantityIsAvailable(PRODUCTION_RUN_ID, QUANTITY);

        assertThatThrownBy(() ->
                useCase.execute(PRODUCTION_RUN_ID, DISTRIBUTOR_ID, QUANTITY))
                .isInstanceOf(InsufficientInventoryException.class);
    }

    @Test
    void createsAllocation_whenQuantityIsAvailable() {
        when(allocationCommandApi.createAllocation(
                PRODUCTION_RUN_ID,
                DISTRIBUTOR_ID,
                QUANTITY
        )).thenReturn(expectedAllocation);

        var result = useCase.execute(PRODUCTION_RUN_ID, DISTRIBUTOR_ID, QUANTITY);

        assertThat(result).isEqualTo(expectedAllocation);
    }
}
