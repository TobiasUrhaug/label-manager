ALTER TABLE channel_allocation
ADD COLUMN units_sold INT NOT NULL DEFAULT 0 CHECK (units_sold >= 0);
