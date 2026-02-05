package org.omt.labelmanager.inventory.application;

import java.time.LocalDate;
import java.util.List;
import org.omt.labelmanager.catalog.domain.release.ReleaseFormat;
import org.omt.labelmanager.inventory.domain.Inventory;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryEntity;
import org.omt.labelmanager.inventory.infrastructure.persistence.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryCRUDHandler.class);

    private final InventoryRepository inventoryRepository;

    public InventoryCRUDHandler(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public Inventory create(
            Long releaseId,
            ReleaseFormat format,
            String description,
            String manufacturer,
            LocalDate manufacturingDate,
            int quantity
    ) {
        log.info("Creating inventory for release {} - {} x{}", releaseId, format, quantity);

        InventoryEntity entity = new InventoryEntity(
                releaseId,
                format,
                description,
                manufacturer,
                manufacturingDate,
                quantity
        );

        entity = inventoryRepository.save(entity);
        log.debug("Inventory created with id {}", entity.getId());

        return Inventory.fromEntity(entity);
    }

    public List<Inventory> findByReleaseId(Long releaseId) {
        return inventoryRepository.findByReleaseId(releaseId).stream()
                .map(Inventory::fromEntity)
                .toList();
    }

    @Transactional
    public boolean delete(Long id) {
        if (!inventoryRepository.existsById(id)) {
            log.warn("Inventory with id {} not found for deletion", id);
            return false;
        }

        inventoryRepository.deleteById(id);
        log.info("Deleted inventory with id {}", id);
        return true;
    }
}
