-- Replace UUID-based meditation_sessions schema with mobile-aligned model
ALTER TABLE meditation_sessions DROP COLUMN topic_id;
ALTER TABLE meditation_sessions DROP COLUMN duration_minutes;
ALTER TABLE meditation_sessions DROP COLUMN completed;

ALTER TABLE meditation_sessions ADD COLUMN topic_id         VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE meditation_sessions ADD COLUMN meditation_title VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE meditation_sessions ADD COLUMN duration_seconds         INT NOT NULL DEFAULT 0;
ALTER TABLE meditation_sessions ADD COLUMN actual_duration_seconds  INT NOT NULL DEFAULT 0;
ALTER TABLE meditation_sessions ADD COLUMN completed_at            BIGINT NOT NULL DEFAULT 0;
ALTER TABLE meditation_sessions ADD COLUMN feeling_rating           INT;
ALTER TABLE meditation_sessions ADD COLUMN sounds_used             TEXT;

-- Drop temporary defaults (columns are now non-null by constraint, defaults only needed for migration)
ALTER TABLE meditation_sessions ALTER COLUMN topic_id          DROP DEFAULT;
ALTER TABLE meditation_sessions ALTER COLUMN meditation_title   DROP DEFAULT;
ALTER TABLE meditation_sessions ALTER COLUMN duration_seconds   DROP DEFAULT;
ALTER TABLE meditation_sessions ALTER COLUMN actual_duration_seconds DROP DEFAULT;
ALTER TABLE meditation_sessions ALTER COLUMN completed_at       DROP DEFAULT;
