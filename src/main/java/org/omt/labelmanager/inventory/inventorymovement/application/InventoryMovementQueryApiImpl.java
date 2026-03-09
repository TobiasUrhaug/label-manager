package org.omt.labelmanager.inventory.inventorymovement.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.omt.labelmanager.inventory.domain.LocationType;
import org.omt.labelmanager.inventory.inventorymovement.api.InventoryMovementQueryApi;
import org.omt.labelmanager.inventory.inventorymovement.domain.InventoryMovement;
import org.omt.labelmanager.inventory.inventorymovement.infrastructure.InventoryMovementRepository;
import org.springframework.stereotype.Service;

@Service
class InventoryMovementQueryApiImpl implements InventoryMovementQueryApi {

    private final InventoryMovementRepository repository;

    InventoryMovementQueryApiImpl(InventoryMovementRepository repository) {
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

    /** For WAREHOUSE (locationId=null) we match any movement to/from WAREHOUSE. */
    private boolean locationIdMatches(Long actual, Long expected) {
        if (expected == null) {
            return true;
        }
        return expected.equals(actual);
    }
}
