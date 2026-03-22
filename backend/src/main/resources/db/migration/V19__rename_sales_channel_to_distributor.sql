-- Rename sales_channel table to distributor
ALTER TABLE sales_channel RENAME TO distributor;

-- Rename index
ALTER INDEX idx_sales_channel_label_id RENAME TO idx_distributor_label_id;

-- Rename sales_channel_id column to distributor_id in channel_allocation
ALTER TABLE channel_allocation
    DROP CONSTRAINT channel_allocation_sales_channel_id_fkey;

ALTER TABLE channel_allocation
    RENAME COLUMN sales_channel_id TO distributor_id;

ALTER TABLE channel_allocation
    ADD CONSTRAINT channel_allocation_distributor_id_fkey
        FOREIGN KEY (distributor_id) REFERENCES distributor(id) ON DELETE CASCADE;

-- Rename index for channel_allocation
ALTER INDEX idx_channel_allocation_sales_channel_id
    RENAME TO idx_channel_allocation_distributor_id;

-- Rename sales_channel_id column to distributor_id in inventory_movement
ALTER TABLE inventory_movement
    DROP CONSTRAINT inventory_movement_sales_channel_id_fkey;

ALTER TABLE inventory_movement
    RENAME COLUMN sales_channel_id TO distributor_id;

ALTER TABLE inventory_movement
    ADD CONSTRAINT inventory_movement_distributor_id_fkey
        FOREIGN KEY (distributor_id) REFERENCES distributor(id) ON DELETE CASCADE;

-- Rename index for inventory_movement
ALTER INDEX idx_inventory_movement_sales_channel_id
    RENAME TO idx_inventory_movement_distributor_id;
