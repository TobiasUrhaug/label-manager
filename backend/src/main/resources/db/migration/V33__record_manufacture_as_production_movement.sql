-- V33: Record manufacture as an inventory movement.
--
-- Until now a production run's manufactured quantity lived only in production_run.quantity, and
-- every caller that wanted warehouse stock had to add it back to the movement sum by hand
-- (AllocateUseCase, ProductionRunController). One PRODUCTION movement per run makes every location
-- balance uniformly Σ in − Σ out, with no caller-side correction.
--
-- EXTERNAL is the counterparty: units enter the system from the pressing plant.
--
-- occurred_at is the manufacturing date, not NOW(): manufacture is the one movement that happened
-- before anyone recorded it, and stamping it at migration time would put every backfilled run's
-- first event after the allocations and sales that followed it. The date is cast to a plain
-- timestamp and only then read AS UTC — a bare ::timestamptz, or AT TIME ZONE on the date itself,
-- resolves against whatever TimeZone the migration session happens to have, putting the movement
-- hours off depending on where it was run. InventoryMovementCommandService.recordManufacture stamps
-- new runs identically.
--
-- Idempotent: re-running inserts nothing, because a run that already has a PRODUCTION movement is
-- excluded. quantity > 0 is guaranteed by production_run's own CHECK constraint.

INSERT INTO inventory_movement (production_run_id,
                                from_location_type,
                                from_location_id,
                                to_location_type,
                                to_location_id,
                                quantity,
                                movement_type,
                                occurred_at,
                                reference_id)
SELECT pr.id,
       'EXTERNAL',
       NULL,
       'WAREHOUSE',
       NULL,
       pr.quantity,
       'PRODUCTION',
       pr.manufacturing_date::timestamp AT TIME ZONE 'UTC',
       NULL
FROM production_run pr
WHERE NOT EXISTS (SELECT 1
                  FROM inventory_movement m
                  WHERE m.production_run_id = pr.id
                    AND m.movement_type = 'PRODUCTION');

-- "At most one PRODUCTION movement per run" is the invariant every balance now rests on, so the
-- database enforces it rather than trusting each write path, and the backfill above stays safe to
-- re-run by hand.
--
-- It does NOT enforce *at least* one. An instance still running the previous version that creates a
-- production run after this migration has committed leaves that run with no PRODUCTION movement, and
-- it reports zero stock forever with no error. This deploy therefore requires that no pre-V33
-- instance serves writes once V33 has run; re-running the INSERT above afterwards repairs any run
-- that slipped through.
CREATE UNIQUE INDEX IF NOT EXISTS idx_inventory_movement_one_production_per_run
    ON inventory_movement (production_run_id)
    WHERE movement_type = 'PRODUCTION';
