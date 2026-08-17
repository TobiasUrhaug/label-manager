package org.omt.labelmanager.inventory.productionrun.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
     * <p>The mutex is coarser than the thing it protects: a balance belongs to a (release, format,
     * location), but the lock is per (release, format), so two distributors selling the same
     * pressing serialise against each other even though their balances are disjoint. Locking per
     * location would need a row per location, which is exactly what the ledger avoids having.
     *
     * <p>Hibernate maps this to {@code FOR NO KEY UPDATE} on PostgreSQL, not {@code FOR UPDATE} —
     * deliberately, so that the {@code FOR KEY SHARE} locks taken by {@code inventory_movement}'s
     * foreign key checks do not block on it. It still conflicts with itself, which is all the mutex
     * needs.
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

    /**
     * One pressing, locked for the rest of the transaction — the same mutex, for the paths that
     * already know which run they are drawing from (allocation, reservation cancellation).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductionRunEntity p WHERE p.id = :productionRunId")
    Optional<ProductionRunEntity> lockById(@Param("productionRunId") Long productionRunId);
}
