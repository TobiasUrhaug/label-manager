CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    release_id BIGINT NOT NULL REFERENCES release(id) ON DELETE CASCADE,
    format VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    manufacturer VARCHAR(255) NOT NULL,
    manufacturing_date DATE NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_inventory_release_id ON inventory(release_id);
