package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorEntity;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelAllocationPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ChannelAllocationRepository channelAllocationRepository;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private LabelTestHelper labelTestHelper;

    private Long productionRunId;
    private Long distributorId;

    @BeforeEach
    void setUp() {
        channelAllocationRepository.deleteAll();
        productionRunRepository.deleteAll();
        distributorRepository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        Long releaseId = releaseTestHelper.createReleaseEntity(
                "Test Release", label.id());

        ProductionRunEntity productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId,
                        ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        500
                ));
        productionRunId = productionRun.getId();

        DistributorEntity distributor = distributorRepository.save(
                new DistributorEntity(label.id(), "Direct Sales", ChannelType.DIRECT));
        distributorId = distributor.getId();
    }

    @Test
    void savesAndRetrievesChannelAllocation() {
        Instant allocatedAt = Instant.parse("2025-06-15T10:00:00Z");
        var entity = new ChannelAllocationEntity(
                productionRunId, distributorId, 100, allocatedAt);

        var saved = channelAllocationRepository.save(entity);

        var retrieved = channelAllocationRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getProductionRunId()).isEqualTo(productionRunId);
        assertThat(retrieved.get().getDistributorId()).isEqualTo(distributorId);
        assertThat(retrieved.get().getQuantity()).isEqualTo(100);
        assertThat(retrieved.get().getAllocatedAt()).isEqualTo(allocatedAt);
    }

    @Test
    void findsByProductionRunId() {
        Instant allocatedAt = Instant.now();
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, distributorId, 100, allocatedAt));
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, distributorId, 50, allocatedAt));

        var allocations = channelAllocationRepository.findByProductionRunId(productionRunId);

        assertThat(allocations).hasSize(2);
        assertThat(allocations)
                .allMatch(a -> a.getProductionRunId().equals(productionRunId));
    }

    @Test
    void sumQuantityByProductionRunIdReturnsTotal() {
        Instant allocatedAt = Instant.now();
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, distributorId, 100, allocatedAt));
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, distributorId, 150, allocatedAt));

        int total = channelAllocationRepository.sumQuantityByProductionRunId(productionRunId);

        assertThat(total).isEqualTo(250);
    }

    @Test
    void sumQuantityByProductionRunIdReturnsZeroWhenNoAllocations() {
        int total = channelAllocationRepository.sumQuantityByProductionRunId(productionRunId);

        assertThat(total).isZero();
    }

    @Test
    void deletesAllocationWhenProductionRunDeleted() {
        Instant allocatedAt = Instant.now();
        channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, distributorId, 100, allocatedAt));

        assertThat(channelAllocationRepository.findByProductionRunId(productionRunId)).hasSize(1);

        productionRunRepository.deleteById(productionRunId);

        assertThat(channelAllocationRepository.findByProductionRunId(productionRunId)).isEmpty();
    }

    @Test
    void findsByProductionRunIdAndDistributorId() {
        Instant allocatedAt = Instant.now();
        var allocation = channelAllocationRepository.save(
                new ChannelAllocationEntity(productionRunId, distributorId, 100, allocatedAt));

        var found = channelAllocationRepository.findByProductionRunIdAndDistributorId(
                productionRunId, distributorId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(allocation.getId());
        assertThat(found.get().getQuantity()).isEqualTo(100);
    }

    @Test
    void findByProductionRunIdAndDistributorId_returnsEmpty_whenNotFound() {
        var found = channelAllocationRepository.findByProductionRunIdAndDistributorId(
                productionRunId, 99999L);

        assertThat(found).isEmpty();
    }
}
