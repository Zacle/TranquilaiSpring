-- Align journal_entries with mobile JournalEntry model
ALTER TABLE journal_entries DROP COLUMN IF EXISTS title;

ALTER TABLE journal_entries
    RENAME COLUMN mood_score TO mood;

ALTER TABLE journal_entries ADD COLUMN prompt_id_new   VARCHAR(100);
UPDATE journal_entries SET prompt_id_new = prompt_id::TEXT WHERE prompt_id IS NOT NULL;
ALTER TABLE journal_entries DROP COLUMN prompt_id;
ALTER TABLE journal_entries RENAME COLUMN prompt_id_new TO prompt_id;

ALTER TABLE journal_entries ADD COLUMN prompt_text   TEXT;
ALTER TABLE journal_entries ADD COLUMN ai_insights   TEXT;
ALTER TABLE journal_entries ADD COLUMN emotional_tone VARCHAR(100);
