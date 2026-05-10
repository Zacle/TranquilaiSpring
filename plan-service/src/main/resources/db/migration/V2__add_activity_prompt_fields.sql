ALTER TABLE plan_activities
    ADD COLUMN IF NOT EXISTS prompt TEXT,
    ADD COLUMN IF NOT EXISTS completion_rating INT,
    ADD COLUMN IF NOT EXISTS completion_notes TEXT;
