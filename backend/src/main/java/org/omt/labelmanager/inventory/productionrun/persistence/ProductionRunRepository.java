package org.omt.labelmanager.inventory.productionrun.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.omt.labelmanager.shared.Format;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionRunRepository extends JpaRepository<ProductionRunEntity, Long> {

    List<ProductionRunEntity> findByReleaseId(Long releaseId);

    List<ProductionRunEntity> findByReleaseIdIn(Collection<Long> releaseIds);

    List<ProductionRunEntity> findByReleaseIdAndFormat(Long releaseId, Format format);

    /**
     * The same pressings, locked for the rest of the transaction.
     *
     * <p>Stock is checked by summing the movement ledger and then inserting a movement, which is a
     * read-then-write across two statements: two concurrent sales of the last 10 units would both
     * read 10 and both succeed. There is no row to lock for a balance — it is a sum — so the
     * production run itself is the mutex, and every writer that draws from a pressing takes it
     * before reading. The second transaction blocks until the first commits, then sums a ledger
     * that includes the first sale.
     *
     * <p>Ordered by id so two transactions locking the same pressings always take them in the same
     * order, which is what stops them deadlocking against each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT p FROM ProductionRunEntity p
            WHERE p.releaseId = :releaseId AND p.format = :format
            ORDER BY p.id
            """)
    List<ProductionRunEntity> lockByReleaseIdAndFormat(
            @Param("releaseId") Long releaseId, @Param("format") Format format);
}
