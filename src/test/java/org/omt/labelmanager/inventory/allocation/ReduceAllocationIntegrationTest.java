package org.omt.labelmanager.inventory.allocation;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.AbstractIntegrationTest;
import org.omt.labelmanager.catalog.label.LabelTestHelper;
import org.omt.labelmanager.catalog.release.ReleaseTestHelper;
import org.omt.labelmanager.catalog.release.domain.ReleaseFormat;
import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.distribution.distributor.infrastructure.DistributorRepository;
import org.omt.labelmanager.inventory.allocation.api.AllocationCommandApi;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationEntity;
import org.omt.labelmanager.inventory.allocation.infrastructure.ChannelAllocationRepository;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunEntity;
import org.omt.labelmanager.inventory.productionrun.infrastructure.ProductionRunRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReduceAllocationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AllocationCommandApi allocationCommandApi;

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
    private Long distributorId;

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
        distributorId = directDistributor.getId();

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
    void reduceAllocation_incrementsUnitsSold() {
        // Create allocation
        var allocationEntity = channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        distributorId,
                        200,
                        Instant.now()
                ));

        allocationCommandApi.reduceAllocation(productionRunId, distributorId, 50);

        var updated = channelAllocationRepository.findById(allocationEntity.getId())
                .orElseThrow();
        assertThat(updated.getUnitsSold()).isEqualTo(50);
    }

    @Test
    void reduceAllocation_allowsMultipleReductions() {
        channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        distributorId,
                        200,
                        Instant.now()
                ));

        allocationCommandApi.reduceAllocation(productionRunId, distributorId, 30);
        allocationCommandApi.reduceAllocation(productionRunId, distributorId, 20);

        var allocation = channelAllocationRepository
                .findByProductionRunIdAndDistributorId(productionRunId, distributorId)
                .orElseThrow();
        assertThat(allocation.getUnitsSold()).isEqualTo(50);
    }

    @Test
    void reduceAllocation_throwsExceptionWhenInsufficientQuantity() {
        channelAllocationRepository.save(
                new ChannelAllocationEntity(
                        productionRunId,
                        distributorId,
                        100,
                        Instant.now()
                ));

        assertThatThrownBy(() ->
                allocationCommandApi.reduceAllocation(productionRunId, distributorId, 150)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient quantity");
    }

    @Test
    void reduceAllocation_throwsExceptionWhenAllocationNotFound() {
        assertThatThrownBy(() ->
                allocationCommandApi.reduceAllocation(productionRunId, distributorId, 50)
        )
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No allocation found");
    }
}
