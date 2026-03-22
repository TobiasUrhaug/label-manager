CREATE TABLE distributor_return (
    id             BIGSERIAL PRIMARY KEY,
    label_id       BIGINT NOT NULL REFERENCES label(id) ON DELETE CASCADE,
    distributor_id BIGINT NOT NULL REFERENCES distributor(id),
    return_date    DATE NOT NULL,
    notes          TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_distributor_return_label_id
    ON distributor_return(label_id);
CREATE INDEX idx_distributor_return_distributor_id
    ON distributor_return(distributor_id);
CREATE INDEX idx_distributor_return_date
    ON distributor_return(return_date);

CREATE TABLE distributor_return_line_item (
    id         BIGSERIAL PRIMARY KEY,
    return_id  BIGINT NOT NULL REFERENCES distributor_return(id) ON DELETE CASCADE,
    release_id BIGINT NOT NULL REFERENCES release(id),
    format     VARCHAR(20) NOT NULL,
    quantity   INT NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_return_line_item_return_id
    ON distributor_return_line_item(return_id);
CREATE INDEX idx_return_line_item_release_id
    ON distributor_return_line_item(release_id);
