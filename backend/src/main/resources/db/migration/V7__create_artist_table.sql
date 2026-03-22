CREATE TABLE artist (
    id BIGSERIAL PRIMARY KEY,
    artist_name VARCHAR(255) NOT NULL,
    real_name VARCHAR(255),
    email VARCHAR(255),
    street VARCHAR(255),
    street2 VARCHAR(255),
    city VARCHAR(255),
    postal_code VARCHAR(50),
    country VARCHAR(100)
);
