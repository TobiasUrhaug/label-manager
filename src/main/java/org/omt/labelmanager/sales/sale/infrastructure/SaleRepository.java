package org.omt.labelmanager.sales.sale.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SaleRepository extends JpaRepository<SaleEntity, Long> {

    List<SaleEntity> findByLabelIdOrderBySaleDateDesc(Long labelId);

    List<SaleEntity> findByDistributorIdOrderBySaleDateDesc(Long distributorId);

    /**
     * Returns all sales recorded against the given production run.
     *
     * <p>Joins through {@code inventory_movement} (where {@code movement_type = 'SALE'}
     * and {@code reference_id} is the sale ID), so results are scoped to the exact
     * production run — not to all runs sharing the same {@code release_id + format}.
     * This is correct even when a release has multiple production runs (repressings).
     */
    @Query(value = """
            SELECT DISTINCT s.*
            FROM sale s
            JOIN inventory_movement im
              ON im.reference_id = s.id
              AND im.movement_type = 'SALE'
            WHERE im.production_run_id = :productionRunId
            ORDER BY s.sale_date DESC
            """, nativeQuery = true)
    List<SaleEntity> findByProductionRunIdOrderBySaleDateDesc(
            @Param("productionRunId") Long productionRunId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s "
            + "WHERE s.labelId = :labelId")
    BigDecimal sumTotalAmountByLabelId(@Param("labelId") Long labelId);
}
