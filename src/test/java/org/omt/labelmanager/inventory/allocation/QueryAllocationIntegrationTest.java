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
import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.allocation.domain.ChannelAllocation;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryAllocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AllocationQueryApi allocationQueryApi;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private ProductionRunRepository productionRunRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private ChannelAllocationRepository channelAllocationRepository;

    private Long labelId;
    private Long releaseId;
    private Long productionRunId;
    private Long distributor1Id;
    private Long distributor2Id;

    @BeforeEach
    void setUp() {
        // Clean up
        channelAllocationRepository.deleteAll();
        productionRunRepository.deleteAll();

        // Create label with DIRECT distributor
        var label = labelTestHelper.createLabelWithDirectDistributor("Test Label");
        labelId = label.id();

        // Get the DIRECT distributor
        var directDistributor = distributorRepository
                .findByLabelIdAndChannelType(labelId, ChannelType.DIRECT)
                .orElseThrow();
        distributor1Id = directDistributor.getId();

        // Create a second distributor
        var distributor2 = distributorRepository.save(
                new DistributorEntity(labelId, "Big Cartel", ChannelType.DISTRIBUTOR)
        );
        distributor2Id = distributor2.getId();

        // Create release
        releaseId = releaseTestHelper.createReleaseEntity("Test Release", labelId);

        // Create production run
        var productionRun = productionRunRepository.save(
                new ProductionRunEntity(
                        releaseId,
                        ReleaseFormat.VINYL,
                        "First pressing",
                        "Plant A",
                        LocalDate.of(2025, 1, 1),
                        500
                ));
        productionRunId = productionRun.getId();
    }

    @Test
    void getAllocationsForProductionRun_returnsAllAllocations() {
        // Create allocations
        channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        distributor1Id,
                        200,
                        Instant.now()
                ));
        channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        distributor2Id,
                        150,
                        Instant.now()
                ));

        List<ChannelAllocation> allocations =
                allocationQueryApi.getAllocationsForProductionRun(productionRunId);

        assertThat(allocations).hasSize(2);
        assertThat(allocations)
                .extracting(ChannelAllocation::quantity)
                .containsExactlyInAnyOrder(200, 150);
    }

    @Test
    void getAllocationsForProductionRun_returnsEmptyListWhenNoAllocations() {
        List<ChannelAllocation> allocations =
                allocationQueryApi.getAllocationsForProductionRun(productionRunId);

        assertThat(allocations).isEmpty();
    }

    @Test
    void getTotalAllocated_returnsSum() {
        channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        distributor1Id,
                        200,
                        Instant.now()
                ));
        channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        distributor2Id,
                        150,
                        Instant.now()
                ));

        int total = allocationQueryApi.getTotalAllocated(productionRunId);

        assertThat(total).isEqualTo(350);
    }

    @Test
    void getTotalAllocated_returnsZeroWhenNoAllocations() {
        int total = allocationQueryApi.getTotalAllocated(productionRunId);

        assertThat(total).isZero();
    }
}
