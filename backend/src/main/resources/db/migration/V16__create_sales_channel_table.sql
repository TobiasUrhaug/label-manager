CREATE TABLE sales_channel (
    id BIGSERIAL PRIMARY KEY,
    label_id BIGINT NOT NULL REFERENCES label(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    channel_type VARCHAR(20) NOT NULL
);

CREATE INDEX idx_sales_channel_label_id ON sales_channel(label_id);
