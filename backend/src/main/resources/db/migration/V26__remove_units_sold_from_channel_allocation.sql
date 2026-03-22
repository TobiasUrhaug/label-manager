-- Units sold is now tracked via inventory_movement records.
-- The channel_allocation table only tracks the original allocation quantity.
ALTER TABLE channel_allocation DROP COLUMN units_sold;
