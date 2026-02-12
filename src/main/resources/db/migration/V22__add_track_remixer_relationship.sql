CREATE TABLE track_remixer (
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    artist_id BIGINT NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
    PRIMARY KEY (track_id, artist_id)
);
