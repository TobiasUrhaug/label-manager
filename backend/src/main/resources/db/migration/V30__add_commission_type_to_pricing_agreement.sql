ALTER TABLE pricing_agreement RENAME COLUMN commission_percentage TO commission_value;
ALTER TABLE pricing_agreement ADD COLUMN commission_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE';
ALTER TABLE pricing_agreement ADD CONSTRAINT chk_commission_type CHECK (commission_type IN ('PERCENTAGE', 'FIXED_AMOUNT'));
ALTER TABLE pricing_agreement ALTER COLUMN commission_type DROP DEFAULT;
