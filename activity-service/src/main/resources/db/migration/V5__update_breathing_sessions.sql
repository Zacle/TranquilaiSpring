-- Align breathing_sessions with mobile BreathingSession model
ALTER TABLE breathing_sessions DROP COLUMN exercise_id;
ALTER TABLE breathing_sessions DROP COLUMN duration_seconds;
ALTER TABLE breathing_sessions DROP COLUMN completed;

ALTER TABLE breathing_sessions ADD COLUMN exercise_id               VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE breathing_sessions ADD COLUMN exercise_title            VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE breathing_sessions ADD COLUMN selected_duration_seconds INT         NOT NULL DEFAULT 0;
ALTER TABLE breathing_sessions ADD COLUMN actual_duration_seconds   INT         NOT NULL DEFAULT 0;
ALTER TABLE breathing_sessions ADD COLUMN completed_cycles          INT         NOT NULL DEFAULT 0;
ALTER TABLE breathing_sessions ADD COLUMN completed_at              BIGINT      NOT NULL DEFAULT 0;
ALTER TABLE breathing_sessions ADD COLUMN feeling_rating            INT;

ALTER TABLE breathing_sessions ALTER COLUMN exercise_id               DROP DEFAULT;
ALTER TABLE breathing_sessions ALTER COLUMN exercise_title            DROP DEFAULT;
ALTER TABLE breathing_sessions ALTER COLUMN selected_duration_seconds DROP DEFAULT;
ALTER TABLE breathing_sessions ALTER COLUMN actual_duration_seconds   DROP DEFAULT;
ALTER TABLE breathing_sessions ALTER COLUMN completed_cycles          DROP DEFAULT;
ALTER TABLE breathing_sessions ALTER COLUMN completed_at              DROP DEFAULT;
