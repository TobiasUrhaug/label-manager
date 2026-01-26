CREATE TABLE cost (
    id BIGSERIAL PRIMARY KEY,
    currency VARCHAR(4) NOT NULL,
    net_amount DECIMAL(19, 2) NOT NULL,
    vat_amount DECIMAL(19, 2) NOT NULL,
    vat_rate DECIMAL(5, 4) NOT NULL,
    gross_amount DECIMAL(19, 2) NOT NULL,
    cost_type VARCHAR(50) NOT NULL,
    incurred_on DATE NOT NULL,
    description VARCHAR(500),
    owner_type VARCHAR(20) NOT NULL,
    owner_id BIGINT NOT NULL,
    document_reference VARCHAR(255)
);

CREATE INDEX idx_cost_owner ON cost(owner_type, owner_id);
