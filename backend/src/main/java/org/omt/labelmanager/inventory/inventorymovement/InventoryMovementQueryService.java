package org.omt.labelmanager.inventory.inventorymovement;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.omt.labelmanager.inventory.InventoryLocation;
import org.omt.labelmanager.inventory.LocationType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.api.LocationBalance;
import org.omt.labelmanager.inventory.inventorymovement.persistence.InventoryMovementRepository;
import org.springframework.stereotype.Service;

@Service
class InventoryMovementQueryService implements InventoryMovementQueryApi {

    private final InventoryMovementRepository repository;

    InventoryMovementQueryService(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InventoryMovement> findByProductionRunId(Long productionRunId) {
        return repository.findByProductionRunIdOrderByOccurredAtDesc(productionRunId).stream()
                .map(InventoryMovement::fromEntity)
                .toList();
    }

    @Override
    public Map<Long, List<InventoryMovement>> findByProductionRunIds(
            Collection<Long> productionRunIds) {
        if (productionRunIds.isEmpty()) {
            return Map.of();
        }
        return repository.findByProductionRunIdInOrderByOccurredAtDesc(productionRunIds).stream()
                .map(InventoryMovement::fromEntity)
                .collect(Collectors.groupingBy(InventoryMovement::productionRunId));
    }

    @Override
    public List<LocationBalance> balancesFor(Collection<Long> productionRunIds) {
        if (productionRunIds.isEmpty()) {
            return List.of();
        }
        return repository.findLocationBalances(productionRunIds).stream()
                .map(InventoryMovementQueryService::toLocationBalance)
                .toList();
    }

    @Override
    public int getCurrentInventory(Long productionRunId, Long distributorId) {
        return onHandAt(productionRunId, InventoryLocation.distributor(distributorId));
    }

    @Override
    public int getWarehouseInventory(Long productionRunId) {
        return onHandAt(productionRunId, InventoryLocation.warehouse());
    }

    @Override
    public int getBandcampInventory(Long productionRunId) {
        return onHandAt(productionRunId, InventoryLocation.bandcamp());
    }

    @Override
    public List<Long> getProductionRunIdsAllocatedToDistributor(Long distributorId) {
        return repository.findDistinctProductionRunIdsAllocatedToDistributor(distributorId);
    }

    private int onHandAt(Long productionRunId, InventoryLocation location) {
        return balancesFor(List.of(productionRunId)).stream()
                .filter(balance -> balance.isAt(location))
                .mapToInt(LocationBalance::onHand)
                .sum();
    }

    /** Row shape: {@code (production_run_id, location_type, location_id, on_hand)}. */
    private static LocationBalance toLocationBalance(Object[] row) {
        return new LocationBalance(
                ((Number) row[0]).longValue(),
                new InventoryLocation(
                        LocationType.valueOf((String) row[1]),
                        row[2] == null ? null : ((Number) row[2]).longValue()),
                ((Number) row[3]).intValue());
    }
}
