CREATE TABLE pricing_agreement (
    id                    BIGSERIAL PRIMARY KEY,
    distributor_id        BIGINT NOT NULL REFERENCES distributor(id) ON DELETE CASCADE,
    production_run_id     BIGINT NOT NULL REFERENCES production_run(id) ON DELETE CASCADE,
    unit_price            NUMERIC(10, 2) NOT NULL,
    commission_percentage NUMERIC(5, 2) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pricing_agreement UNIQUE (distributor_id, production_run_id)
);
