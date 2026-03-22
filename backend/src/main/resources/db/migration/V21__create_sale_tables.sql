CREATE TABLE sale (
    id BIGSERIAL PRIMARY KEY,
    label_id BIGINT NOT NULL REFERENCES label(id) ON DELETE CASCADE,
    sale_date DATE NOT NULL,
    channel VARCHAR(50) NOT NULL,
    notes TEXT,
    total_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sale_label_id ON sale(label_id);
CREATE INDEX idx_sale_date ON sale(sale_date);
CREATE INDEX idx_sale_channel ON sale(channel);

CREATE TABLE sale_line_item (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sale(id) ON DELETE CASCADE,
    release_id BIGINT NOT NULL REFERENCES release(id),
    format VARCHAR(20) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19, 2) NOT NULL CHECK (unit_price >= 0),
    line_total DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR'
);

CREATE INDEX idx_sale_line_item_sale_id ON sale_line_item(sale_id);
CREATE INDEX idx_sale_line_item_release_id ON sale_line_item(release_id);
