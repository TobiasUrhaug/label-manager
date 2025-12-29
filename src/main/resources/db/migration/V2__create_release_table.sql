CREATE TABLE release (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    release_date DATE,
    label_id BIGINT NOT NULL,
    CONSTRAINT fk_label FOREIGN KEY (label_id)
        REFERENCES label(id) ON DELETE CASCADE
);
