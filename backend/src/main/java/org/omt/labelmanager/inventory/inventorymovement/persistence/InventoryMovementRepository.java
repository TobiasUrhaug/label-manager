package org.omt.labelmanager.inventory.inventorymovement.persistence;

import java.util.Collection;
import java.util.List;
import org.omt.labelmanager.inventory.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {

    List<InventoryMovementEntity> findByProductionRunIdOrderByOccurredAtDesc(Long productionRunId);

    List<InventoryMovementEntity> findByProductionRunIdInOrderByOccurredAtDesc(
            Collection<Long> productionRunIds);

    void deleteByMovementTypeAndReferenceId(MovementType movementType, Long referenceId);

    @Query(
            """
            SELECT DISTINCT m.productionRunId
            FROM InventoryMovementEntity m
            WHERE m.toLocationType = 'DISTRIBUTOR'
              AND m.toLocationId = :distributorId
              AND m.movementType = 'ALLOCATION'
            """)
    List<Long> findDistinctProductionRunIdsAllocatedToDistributor(
            @Param("distributorId") Long distributorId);

    /**
     * Every location balance for these runs, in one query.
     *
     * <p>Each movement contributes twice — plus to where it went, minus to where it came from — so
     * summing the legs gives {@code Σ in − Σ out} per location without loading a single row into
     * memory. This replaces reading every movement for a run and summing it in Java, which cost one
     * query per run per location on a release page.
     *
     * <p>Native because JPQL has no {@code UNION}, and expressing the two legs any other way means
     * one aggregate per location type.
     *
     * @return rows of {@code (production_run_id, location_type, location_id, on_hand)}; locations
     *     whose balance nets to zero are omitted
     */
    @Query(
            value =
                    """
                    SELECT leg.production_run_id, leg.location_type, leg.location_id,
                           SUM(leg.delta) AS on_hand
                    FROM (SELECT production_run_id,
                                 to_location_type AS location_type,
                                 to_location_id   AS location_id,
                                 quantity         AS delta
                          FROM inventory_movement
                          WHERE production_run_id IN (:productionRunIds)
                          UNION ALL
                          SELECT production_run_id,
                                 from_location_type,
                                 from_location_id,
                                 -quantity
                          FROM inventory_movement
                          WHERE production_run_id IN (:productionRunIds)) leg
                    GROUP BY leg.production_run_id, leg.location_type, leg.location_id
                    HAVING SUM(leg.delta) <> 0
                    """,
            nativeQuery = true)
    List<Object[]> findLocationBalances(
            @Param("productionRunIds") Collection<Long> productionRunIds);
}
