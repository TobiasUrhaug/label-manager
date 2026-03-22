-- Migrate RETAIL to RECORD_STORE (semantically similar)
UPDATE distributor SET channel_type = 'RECORD_STORE' WHERE channel_type = 'RETAIL';
UPDATE sale SET channel = 'RECORD_STORE' WHERE channel = 'RETAIL';

-- Migrate WAREHOUSE to DISTRIBUTOR (closest match)
UPDATE distributor SET channel_type = 'DISTRIBUTOR' WHERE channel_type = 'WAREHOUSE';
UPDATE sale SET channel = 'DISTRIBUTOR' WHERE channel = 'WAREHOUSE';
