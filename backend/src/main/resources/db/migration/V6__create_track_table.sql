CREATE TABLE track (
    id BIGSERIAL PRIMARY KEY,
    artist VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    duration_seconds INTEGER NOT NULL,
    position INTEGER NOT NULL,
    release_id BIGINT NOT NULL,
    CONSTRAINT fk_release FOREIGN KEY (release_id)
        REFERENCES release(id) ON DELETE CASCADE
);
