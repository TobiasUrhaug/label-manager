-- app_user.created_at was the only timestamp in the schema without a time zone; sale,
-- distributor_return, pricing_agreement and inventory_movement all use TIMESTAMPTZ.
--
-- The column is mapped to a java.time.Instant (UserEntity.createdAt), which is an absolute
-- point in time. Storing it in a zoneless column meant the value was written as server-local
-- time and reinterpreted as local time on read, so the mapping was lossy.
--
-- Existing values were written by CURRENT_TIMESTAMP / Instant.now() in the server's zone, so
-- interpret them in that zone when converting.
ALTER TABLE app_user
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE
        USING created_at AT TIME ZONE current_setting('TimeZone');

ALTER TABLE app_user
    ALTER COLUMN created_at SET DEFAULT NOW();
