package org.omt.labelmanager.sales.sale.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

interface SaleRepository extends JpaRepository<SaleEntity, Long> {

    List<SaleEntity> findByLabelIdOrderBySaleDateDesc(Long labelId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SaleEntity s "
            + "WHERE s.labelId = :labelId")
    BigDecimal sumTotalAmountByLabelId(@Param("labelId") Long labelId);
}
