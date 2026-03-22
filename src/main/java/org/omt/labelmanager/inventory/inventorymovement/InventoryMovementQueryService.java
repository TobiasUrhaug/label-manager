package org.omt.labelmanager.inventory.inventorymovement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.omt.labelmanager.inventory.LocationType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
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
        return movementsFor(productionRunId);
    }

    @Override
    public List<InventoryMovement> getMovementsForProductionRun(Long productionRunId) {
        return movementsFor(productionRunId);
    }

    @Override
    public int getCurrentInventory(Long productionRunId, Long distributorId) {
        var movements = movementsFor(productionRunId);
        int inbound = sumQuantityTo(movements, LocationType.DISTRIBUTOR, distributorId);
        int outbound = sumQuantityFrom(movements, LocationType.DISTRIBUTOR, distributorId);
        return inbound - outbound;
    }

    @Override
    public int getWarehouseInventory(Long productionRunId) {
        var movements = movementsFor(productionRunId);
        int inbound = sumQuantityTo(movements, LocationType.WAREHOUSE, null);
        int outbound = sumQuantityFrom(movements, LocationType.WAREHOUSE, null);
        return inbound - outbound;
    }

    @Override
    public List<Long> getProductionRunIdsAllocatedToDistributor(Long distributorId) {
        return repository.findDistinctProductionRunIdsAllocatedToDistributor(distributorId);
    }

    @Override
    public int getBandcampInventory(Long productionRunId) {
        var movements = movementsFor(productionRunId);
        int inbound = sumQuantityTo(movements, LocationType.BANDCAMP, null);
        int outbound = sumQuantityFrom(movements, LocationType.BANDCAMP, null);
        return inbound - outbound;
    }

    @Override
    public Map<Long, Integer> getCurrentInventoryByDistributor(Long productionRunId) {
        var movements = movementsFor(productionRunId);

        // Collect all distinct distributor IDs that appear in any movement
        var distributorIds = movements.stream()
                .flatMap(m -> {
                    Stream.Builder<Long> ids = Stream.builder();
                    if (m.fromLocationType() == LocationType.DISTRIBUTOR
                            && m.fromLocationId() != null) {
                        ids.add(m.fromLocationId());
                    }
                    if (m.toLocationType() == LocationType.DISTRIBUTOR
                            && m.toLocationId() != null) {
                        ids.add(m.toLocationId());
                    }
                    return ids.build();
                })
                .collect(Collectors.toSet());

        return distributorIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> sumQuantityTo(movements, LocationType.DISTRIBUTOR, id)
                                - sumQuantityFrom(movements, LocationType.DISTRIBUTOR, id)
                ))
                .entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<InventoryMovement> movementsFor(Long productionRunId) {
        return repository.findByProductionRunIdOrderByOccurredAtDesc(productionRunId).stream()
                .map(InventoryMovement::fromEntity)
                .toList();
    }

    private int sumQuantityTo(
            List<InventoryMovement> movements,
            LocationType locationType,
            Long locationId
    ) {
        return movements.stream()
                .filter(m -> m.toLocationType() == locationType
                        && locationIdMatches(m.toLocationId(), locationId))
                .mapToInt(InventoryMovement::quantity)
                .sum();
    }

    private int sumQuantityFrom(
            List<InventoryMovement> movements,
            LocationType locationType,
            Long locationId
    ) {
        return movements.stream()
                .filter(m -> m.fromLocationType() == locationType
                        && locationIdMatches(m.fromLocationId(), locationId))
                .mapToInt(InventoryMovement::quantity)
                .sum();
    }

    /** When no specific location ID is expected (WAREHOUSE, BANDCAMP, EXTERNAL), match by type alone. */
    private boolean locationIdMatches(Long actual, Long expected) {
        if (expected == null) {
            return true;
        }
        return expected.equals(actual);
    }
}
