-- Insert default "Unknown" artist
INSERT INTO artist (artist_name) VALUES ('Unknown');

-- Create join table for release <-> artist (many-to-many)
CREATE TABLE release_artist (
    release_id BIGINT NOT NULL REFERENCES release(id) ON DELETE CASCADE,
    artist_id BIGINT NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
    PRIMARY KEY (release_id, artist_id)
);

-- Create join table for track <-> artist (many-to-many)
CREATE TABLE track_artist (
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    artist_id BIGINT NOT NULL REFERENCES artist(id) ON DELETE CASCADE,
    PRIMARY KEY (track_id, artist_id)
);

-- Remove old string artist column from track
ALTER TABLE track DROP COLUMN artist;
