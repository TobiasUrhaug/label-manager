CREATE TABLE release_format (
    release_id BIGINT NOT NULL REFERENCES release(id) ON DELETE CASCADE,
    format VARCHAR(20) NOT NULL,
    PRIMARY KEY (release_id, format)
);

INSERT INTO release_format (release_id, format)
SELECT id, 'DIGITAL' FROM release;
