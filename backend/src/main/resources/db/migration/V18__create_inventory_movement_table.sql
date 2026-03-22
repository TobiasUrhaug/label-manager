CREATE TABLE inventory_movement (
    id BIGSERIAL PRIMARY KEY,
    production_run_id BIGINT NOT NULL REFERENCES production_run(id) ON DELETE CASCADE,
    sales_channel_id BIGINT NOT NULL REFERENCES sales_channel(id) ON DELETE CASCADE,
    quantity_delta INT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    reference_id BIGINT
);

CREATE INDEX idx_inventory_movement_production_run_id ON inventory_movement(production_run_id);
CREATE INDEX idx_inventory_movement_sales_channel_id ON inventory_movement(sales_channel_id);
