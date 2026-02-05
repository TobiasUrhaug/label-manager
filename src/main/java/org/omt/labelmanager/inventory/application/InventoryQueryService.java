package org.omt.labelmanager.inventory.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.inventory.domain.Inventory;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;

    public InventoryQueryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<Inventory> getInventoryForRelease(Long releaseId) {
        return inventoryRepository.findByReleaseId(releaseId).stream()
                .map(Inventory::fromEntity)
                .toList();
    }

    public Map<ReleaseFormat, Integer> getTotalsForRelease(Long releaseId) {
        return inventoryRepository.findByReleaseId(releaseId).stream()
                .map(Inventory::fromEntity)
                .collect(Collectors.groupingBy(
                        Inventory::format,
                        Collectors.summingInt(Inventory::quantity)
                ));
    }
}
