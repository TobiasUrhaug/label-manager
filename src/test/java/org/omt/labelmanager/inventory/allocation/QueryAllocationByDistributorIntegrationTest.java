package org.omt.labelmanager.inventory.allocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.DistributorTestHelper;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.allocation.api.AllocationQueryApi;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.productionrun.ProductionRunTestHelper;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryAllocationByDistributorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AllocationQueryApi allocationQueryApi;

    @Autowired
    private AllocationCommandApi allocationCommandApi;

    @Autowired
    private ChannelAllocationRepository repository;

    @Autowired
    private LabelTestHelper labelTestHelper;

    @Autowired
    private DistributorTestHelper distributorTestHelper;

    @Autowired
    private ReleaseTestHelper releaseTestHelper;

    @Autowired
    private ProductionRunTestHelper productionRunTestHelper;

    private Long distributorId;
    private Long otherDistributorId;
    private Long productionRunId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        var label = labelTestHelper.createLabel("Test Label");
        var distributor = distributorTestHelper.createDistributor(label.id(), "Distributor A", ChannelType.DISTRIBUTOR);
        distributorId = distributor.id();

        var otherDistributor = distributorTestHelper.createDistributor(label.id(), "Distributor B", ChannelType.DISTRIBUTOR);
        otherDistributorId = otherDistributor.id();

        Long releaseId = releaseTestHelper.createReleaseEntity("Test Album", label.id());
        var run = productionRunTestHelper.createProductionRun(releaseId, ReleaseFormat.VINYL, 300);
        productionRunId = run.id();
    }

    @Test
    void getAllocationsForDistributor_returnsOnlyAllocationsForThatDistributor() {
        allocationCommandApi.createAllocation(productionRunId, distributorId, 100);
        allocationCommandApi.createAllocation(productionRunId, otherDistributorId, 50);

        var allocations = allocationQueryApi.getAllocationsForDistributor(distributorId);

        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).distributorId()).isEqualTo(distributorId);
    }

    @Test
    void getAllocationsForDistributor_returnsEmpty_whenNoAllocationsExist() {
        var allocations = allocationQueryApi.getAllocationsForDistributor(distributorId);

        assertThat(allocations).isEmpty();
    }
}
