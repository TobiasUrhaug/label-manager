-- Create DIRECT distributor for each existing label that doesn't have one
-- This represents the label's own inventory for direct sales
INSERT INTO distributor (label_id, name, channel_type)
SELECT l.id, l.name || ' Direct Sales', 'DIRECT'
FROM label l
WHERE NOT EXISTS (
    SELECT 1 FROM distributor d
    WHERE d.label_id = l.id AND d.channel_type = 'DIRECT'
);

-- Add unique constraint to ensure only one DIRECT distributor per label
CREATE UNIQUE INDEX idx_distributor_label_direct
    ON distributor(label_id)
    WHERE channel_type = 'DIRECT';
