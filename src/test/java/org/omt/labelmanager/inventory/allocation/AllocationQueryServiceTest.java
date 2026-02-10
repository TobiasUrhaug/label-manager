package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.catalog.release.ReleaseFormat;
import org.omt.labelmanager.inventory.infrastructure.persistence.ProductionRunEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.ProductionRunRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationQueryServiceTest {

    @Mock
    private ChannelAllocationRepository channelAllocationRepository;

    @Mock
    private ProductionRunRepository productionRunRepository;

    private AllocationQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new AllocationQueryService(
                channelAllocationRepository,
                productionRunRepository
        );
    }

    @Test
    void getAllocationsForProductionRunReturnsList() {
        Long productionRunId = 1L;
        var entity1 = createAllocationEntity(1L, productionRunId, 2L, 100);
        var entity2 = createAllocationEntity(2L, productionRunId, 3L, 150);
        when(channelAllocationRepository.findByProductionRunId(productionRunId))
                .thenReturn(List.of(entity1, entity2));

        var allocations = queryService.getAllocationsForProductionRun(productionRunId);

        assertThat(allocations).hasSize(2);
        assertThat(allocations.get(0).quantity()).isEqualTo(100);
        assertThat(allocations.get(1).quantity()).isEqualTo(150);
    }

    @Test
    void getTotalAllocatedReturnsSum() {
        Long productionRunId = 1L;
        when(channelAllocationRepository.sumQuantityByProductionRunId(productionRunId))
                .thenReturn(250);

        int total = queryService.getTotalAllocated(productionRunId);

        assertThat(total).isEqualTo(250);
    }

    @Test
    void getUnallocatedQuantityReturnsManufacturedMinusAllocated() {
        Long productionRunId = 1L;
        var productionRun = createProductionRunEntity(productionRunId, 500);
        when(productionRunRepository.findById(productionRunId))
                .thenReturn(Optional.of(productionRun));
        when(channelAllocationRepository.sumQuantityByProductionRunId(productionRunId))
                .thenReturn(200);

        int unallocated = queryService.getUnallocatedQuantity(productionRunId);

        assertThat(unallocated).isEqualTo(300);
    }

    @Test
    void getUnallocatedQuantityReturnsZeroWhenProductionRunNotFound() {
        Long productionRunId = 999L;
        when(productionRunRepository.findById(productionRunId))
                .thenReturn(Optional.empty());
        when(channelAllocationRepository.sumQuantityByProductionRunId(productionRunId))
                .thenReturn(0);

        int unallocated = queryService.getUnallocatedQuantity(productionRunId);

        assertThat(unallocated).isZero();
    }

    private ChannelAllocationEntity createAllocationEntity(
            Long id,
            Long productionRunId,
            Long salesChannelId,
            int quantity
    ) {
        var entity = new ChannelAllocationEntity(
                productionRunId,
                salesChannelId,
                quantity,
                Instant.now()
        );
        try {
            var idField = ChannelAllocationEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entity;
    }

    private ProductionRunEntity createProductionRunEntity(Long id, int quantity) {
        var entity = new ProductionRunEntity(
                1L,
                ReleaseFormat.VINYL,
                "Description",
                "Manufacturer",
                java.time.LocalDate.now(),
                quantity
        );
        try {
            var idField = ProductionRunEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entity;
    }
}
