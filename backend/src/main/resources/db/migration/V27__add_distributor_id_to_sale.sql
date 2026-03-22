-- Add distributor_id column (nullable initially to allow backfill)
ALTER TABLE sale ADD COLUMN distributor_id BIGINT REFERENCES distributor(id);

-- Backfill DIRECT sales: find the DIRECT distributor for each label
UPDATE sale s
SET distributor_id = d.id
FROM distributor d
WHERE d.label_id = s.label_id
  AND d.channel_type = 'DIRECT'
  AND s.channel = 'DIRECT';

-- Backfill non-DIRECT sales: resolve distributor from the SALE movement's from_location_id.
-- The distributor was determined during registration but was never persisted on the sale.
UPDATE sale s
SET distributor_id = (
    SELECT im.from_location_id
    FROM inventory_movement im
    WHERE im.reference_id = s.id
      AND im.movement_type = 'SALE'
      AND im.from_location_type = 'DISTRIBUTOR'
    LIMIT 1
)
WHERE s.channel != 'DIRECT'
  AND s.distributor_id IS NULL;

-- Fallback: any remaining sales still without a distributor_id (e.g. test data created
-- directly without a movement record) are assigned the label's DIRECT distributor.
UPDATE sale s
SET distributor_id = d.id
FROM distributor d
WHERE d.label_id = s.label_id
  AND d.channel_type = 'DIRECT'
  AND s.distributor_id IS NULL;

-- Enforce NOT NULL now that all rows are backfilled
ALTER TABLE sale ALTER COLUMN distributor_id SET NOT NULL;

-- Index for distributor history lookups
CREATE INDEX idx_sale_distributor_id ON sale(distributor_id);
