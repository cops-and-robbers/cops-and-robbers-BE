ALTER TABLE participants
    ALTER COLUMN is_ready SET DEFAULT true;

UPDATE participants
SET is_ready = true
WHERE is_ready = false;
