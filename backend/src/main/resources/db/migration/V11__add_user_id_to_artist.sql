ALTER TABLE artist ADD COLUMN user_id BIGINT REFERENCES app_user(id);
