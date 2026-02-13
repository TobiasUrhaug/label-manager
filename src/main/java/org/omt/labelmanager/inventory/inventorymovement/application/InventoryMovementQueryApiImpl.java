package org.omt.labelmanager.inventory.inventorymovement.application;

import java.util.List;
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
        return repository.findByProductionRunId(productionRunId).stream()
                .map(InventoryMovement::fromEntity)
                .toList();
    }

    @Override
    public List<InventoryMovement> findByDistributorId(Long distributorId) {
        return repository.findByDistributorId(distributorId).stream()
                .map(InventoryMovement::fromEntity)
                .toList();
    }
}
