-- V25: Refactor inventory_movement to a bidirectional from/to location model.
--
-- Old schema: (distributor_id NOT NULL, quantity_delta signed int)
--   - ALLOCATION rows: distributor_id = destination, quantity_delta = +N
--   - SALE rows:       distributor_id = source,      quantity_delta = -N
--
-- New schema: (from_location_type, from_location_id, to_location_type, to_location_id, quantity)
--   All quantities are positive; direction is expressed via from/to location types.

-- 1. Add new columns (nullable initially so we can backfill before constraining)
ALTER TABLE inventory_movement
    ADD COLUMN from_location_type VARCHAR(20),
    ADD COLUMN from_location_id   BIGINT,
    ADD COLUMN to_location_type   VARCHAR(20),
    ADD COLUMN to_location_id     BIGINT,
    ADD COLUMN quantity           INT;

-- 2. Migrate existing ALLOCATION records
--    Units moved FROM warehouse TO the distributor
UPDATE inventory_movement
SET from_location_type = 'WAREHOUSE',
    from_location_id   = NULL,
    to_location_type   = 'DISTRIBUTOR',
    to_location_id     = distributor_id,
    quantity           = quantity_delta
WHERE movement_type = 'ALLOCATION';

-- 3. Migrate existing SALE records
--    Units moved FROM the distributor TO external (sold to customers)
UPDATE inventory_movement
SET from_location_type = 'DISTRIBUTOR',
    from_location_id   = distributor_id,
    to_location_type   = 'EXTERNAL',
    to_location_id     = NULL,
    quantity           = ABS(quantity_delta)
WHERE movement_type = 'SALE';

-- 4. Apply NOT NULL constraints now that all rows are backfilled
ALTER TABLE inventory_movement
    ALTER COLUMN from_location_type SET NOT NULL,
    ALTER COLUMN to_location_type   SET NOT NULL,
    ALTER COLUMN quantity            SET NOT NULL;

-- 5. Drop the old distributor_id index before dropping the column
DROP INDEX IF EXISTS idx_inventory_movement_distributor_id;

-- 6. Drop old columns
ALTER TABLE inventory_movement
    DROP COLUMN distributor_id,
    DROP COLUMN quantity_delta;

-- 7. Add indexes for the new columns
--    Note: idx_inventory_movement_production_run_id already exists from V18 — not recreated.
CREATE INDEX idx_inventory_movement_from_location
    ON inventory_movement(production_run_id, from_location_type, from_location_id);

CREATE INDEX idx_inventory_movement_to_location
    ON inventory_movement(production_run_id, to_location_type, to_location_id);

CREATE INDEX idx_inventory_movement_reference
    ON inventory_movement(movement_type, reference_id);
