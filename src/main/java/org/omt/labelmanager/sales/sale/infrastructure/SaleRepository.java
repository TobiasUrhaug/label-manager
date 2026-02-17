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
     * Returns all sales that contain at least one line item belonging to the given
     * production run, identified by matching release_id + format. Uses a native query
     * because the join crosses module boundaries (production_run is in the inventory
     * context).
     */
    @Query(value = """
            SELECT DISTINCT s.*
            FROM sale s
            JOIN sale_line_item sli ON sli.sale_id = s.id
            JOIN production_run pr
              ON pr.release_id = sli.release_id AND pr.format = sli.format
            WHERE pr.id = :productionRunId
            ORDER BY s.sale_date DESC
            """, nativeQuery = true)
    List<SaleEntity> findByProductionRunIdOrderBySaleDateDesc(
            @Param("productionRunId") Long productionRunId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s "
            + "WHERE s.labelId = :labelId")
    BigDecimal sumTotalAmountByLabelId(@Param("labelId") Long labelId);
}
