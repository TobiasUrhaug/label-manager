package org.omt.labelmanager.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * V33 backfills one PRODUCTION movement per existing production run.
 *
 * <p>It is the first of Phase 4's irreversible data migrations — a wrong backfill silently corrupts
 * every stock figure — so it is verified against a database seeded at V32 rather than assumed. The
 * identity that must hold: each run's post-migration warehouse balance equals the figure callers
 * computed before it, namely {@code production_run.quantity + movement delta}.
 *
 * <p>Runs its own container because the shared one in {@code AbstractIntegrationTest} is migrated
 * straight to head, leaving nothing for V33 to find.
 */
class V33MigrationTest {

    private static final String V33_SCRIPT =
            "/db/migration/V33__record_manufacture_as_production_movement.sql";

    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startContainer() {
        postgres =
                new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("migrationtest")
                        .withUsername("test")
                        .withPassword("test");
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    /** Each test starts from a database at V32, with V33 still ahead of it. */
    @BeforeEach
    void resetToV32() {
        flyway(null).clean();
        migrateTo("32");
    }

    @Test
    void backfillPreservesEveryRunsWarehouseBalance() throws Exception {
        // quantity, then the movements that ran against it: allocated out, returned back in.
        final Long untouched = seedRun(500);
        Long partlyAllocated = seedRun(300);
        allocateOut(partlyAllocated, 120);
        Long allocatedAndReturned = seedRun(100);
        allocateOut(allocatedAndReturned, 100);
        returnIn(allocatedAndReturned, 40);

        Map<Long, Integer> before = warehouseBalancesAsCallersComputedThem();
        assertThat(before)
                .containsEntry(untouched, 500)
                .containsEntry(partlyAllocated, 180)
                .containsEntry(allocatedAndReturned, 40);

        migrateTo("33");

        assertThat(warehouseBalancesFromTheLedgerAlone()).isEqualTo(before);
    }

    /** Bandcamp is the other pair of movements that touches the warehouse. */
    @Test
    void backfillPreservesBalancesForRunsWithBandcampMovements() throws Exception {
        Long reserved = seedRun(400);
        recordMovement(reserved, "WAREHOUSE", "BANDCAMP", 150, "ALLOCATION");
        recordMovement(reserved, "BANDCAMP", "WAREHOUSE", 40, "RETURN");

        Map<Long, Integer> before = warehouseBalancesAsCallersComputedThem();
        assertThat(before).containsEntry(reserved, 290);

        migrateTo("33");

        assertThat(warehouseBalancesFromTheLedgerAlone()).isEqualTo(before);
    }

    @Test
    void backfillStampsTheMovementWithTheManufacturingDate() throws Exception {
        Long run = seedRun(100);

        migrateTo("33");

        // Not the migration's wall clock: manufacture belongs at the head of the run's history,
        // before the allocations and sales that followed it.
        assertThat(productionMovementOccurredAt(run))
                .isEqualTo(LocalDate.of(2025, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    void backfillSkipsOnlyRunsThatAlreadyHaveAProductionMovement() throws Exception {
        Long alreadyRecorded = seedRun(200);
        recordMovement(alreadyRecorded, "EXTERNAL", "WAREHOUSE", 200, "PRODUCTION");
        Long alreadyAllocated = seedRun(300);
        allocateOut(alreadyAllocated, 75);

        migrateTo("33");

        // The guard matches on movement type, not on "has any movement at all" — a run that has
        // been allocated from still needs its manufacture recorded.
        assertThat(productionMovementCount(alreadyRecorded)).isEqualTo(1);
        assertThat(productionMovementCount(alreadyAllocated)).isEqualTo(1);
        assertThat(warehouseBalancesFromTheLedgerAlone())
                .containsEntry(alreadyRecorded, 200)
                .containsEntry(alreadyAllocated, 225);
    }

    @Test
    void backfillIsIdempotent() throws Exception {
        Long run = seedRun(250);
        allocateOut(run, 50);
        migrateTo("33");

        Map<Long, Integer> afterFirstRun = warehouseBalancesFromTheLedgerAlone();

        execute(v33Script());

        assertThat(productionMovementCount(run)).isEqualTo(1);
        assertThat(warehouseBalancesFromTheLedgerAlone()).isEqualTo(afterFirstRun);
    }

    /**
     * The invariant every balance now rests on: one manufacture per run, enforced by the schema.
     */
    @Test
    void schemaRefusesASecondProductionMovement() throws Exception {
        Long run = seedRun(250);
        migrateTo("33");

        assertThatThrownBy(() -> recordMovement(run, "EXTERNAL", "WAREHOUSE", 250, "PRODUCTION"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("idx_inventory_movement_one_production_per_run");
    }

    /**
     * What callers computed before V33: the run's manufactured quantity, plus whatever the ledger
     * says has since moved in or out of the warehouse.
     */
    private Map<Long, Integer> warehouseBalancesAsCallersComputedThem() throws SQLException {
        return balancesByRun(
                """
                SELECT pr.id,
                       pr.quantity + COALESCE(SUM(
                           CASE WHEN m.to_location_type = 'WAREHOUSE' THEN m.quantity
                                WHEN m.from_location_type = 'WAREHOUSE' THEN -m.quantity
                                ELSE 0 END), 0)
                FROM production_run pr
                LEFT JOIN inventory_movement m ON m.production_run_id = pr.id
                GROUP BY pr.id, pr.quantity
                ORDER BY pr.id
                """);
    }

    /** What callers compute after V33: the ledger alone, with no caller-side correction. */
    private Map<Long, Integer> warehouseBalancesFromTheLedgerAlone() throws SQLException {
        return balancesByRun(
                """
                SELECT pr.id,
                       COALESCE(SUM(
                           CASE WHEN m.to_location_type = 'WAREHOUSE' THEN m.quantity
                                WHEN m.from_location_type = 'WAREHOUSE' THEN -m.quantity
                                ELSE 0 END), 0)
                FROM production_run pr
                LEFT JOIN inventory_movement m ON m.production_run_id = pr.id
                GROUP BY pr.id
                ORDER BY pr.id
                """);
    }

    private Map<Long, Integer> balancesByRun(String sql) throws SQLException {
        Map<Long, Integer> balances = new LinkedHashMap<>();
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                balances.put(rs.getLong(1), rs.getInt(2));
            }
        }
        return balances;
    }

    private Instant productionMovementOccurredAt(Long productionRunId) throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet rs =
                        statement.executeQuery(
                                "SELECT occurred_at FROM inventory_movement WHERE production_run_id"
                                        + " = "
                                        + productionRunId
                                        + " AND movement_type = 'PRODUCTION'")) {
            rs.next();
            return rs.getTimestamp(1).toInstant();
        }
    }

    private int productionMovementCount(Long productionRunId) throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet rs =
                        statement.executeQuery(
                                "SELECT COUNT(*) FROM inventory_movement WHERE production_run_id = "
                                        + productionRunId
                                        + " AND movement_type = 'PRODUCTION'")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Long seedRun(int quantity) throws SQLException {
        Long labelId =
                insertReturningId("INSERT INTO label (name) VALUES ('Test Label') RETURNING id");
        Long releaseId =
                insertReturningId(
                        "INSERT INTO release (name, label_id) VALUES ('Test Release', "
                                + labelId
                                + ") RETURNING id");
        return insertReturningId(
                """
                INSERT INTO production_run (release_id, format, description, manufacturer,
                                            manufacturing_date, quantity)
                VALUES (%d, 'VINYL', 'Pressing', 'Plant', DATE '2025-01-01', %d)
                RETURNING id
                """
                        .formatted(releaseId, quantity));
    }

    private void allocateOut(Long productionRunId, int quantity) throws SQLException {
        recordMovement(productionRunId, "WAREHOUSE", "DISTRIBUTOR", quantity, "ALLOCATION");
    }

    private void returnIn(Long productionRunId, int quantity) throws SQLException {
        recordMovement(productionRunId, "DISTRIBUTOR", "WAREHOUSE", quantity, "RETURN");
    }

    private void recordMovement(
            Long productionRunId, String from, String to, int quantity, String movementType)
            throws SQLException {
        execute(
                """
                INSERT INTO inventory_movement (production_run_id, from_location_type,
                                                from_location_id, to_location_type,
                                                to_location_id, quantity, movement_type)
                VALUES (%d, '%s', %s, '%s', %s, %d, '%s')
                """
                        .formatted(
                                productionRunId,
                                from,
                                "DISTRIBUTOR".equals(from) ? "1" : "NULL",
                                to,
                                "DISTRIBUTOR".equals(to) ? "1" : "NULL",
                                quantity,
                                movementType));
    }

    private Long insertReturningId(String sql) throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void migrateTo(String version) {
        flyway(version).migrate();
    }

    private Flyway flyway(String target) {
        var configuration =
                Flyway.configure()
                        .dataSource(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword())
                        .locations("classpath:db/migration")
                        .cleanDisabled(false);
        return (target == null ? configuration : configuration.target(target)).load();
    }

    private String v33Script() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(V33_SCRIPT)) {
            assertThat(in).as("V33 script on the classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
