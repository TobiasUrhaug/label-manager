CREATE TABLE channel_allocation (
    id BIGSERIAL PRIMARY KEY,
    production_run_id BIGINT NOT NULL REFERENCES production_run(id) ON DELETE CASCADE,
    sales_channel_id BIGINT NOT NULL REFERENCES sales_channel(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    allocated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_channel_allocation_production_run_id ON channel_allocation(production_run_id);
CREATE INDEX idx_channel_allocation_sales_channel_id ON channel_allocation(sales_channel_id);
