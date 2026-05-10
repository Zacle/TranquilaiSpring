-- Drop old content tables (CASCADE removes FK constraints automatically)
DROP TABLE IF EXISTS affirmation_favorites CASCADE;
DROP TABLE IF EXISTS breathing_favorites CASCADE;
DROP TABLE IF EXISTS meditation_favorites CASCADE;
DROP TABLE IF EXISTS affirmations CASCADE;
DROP TABLE IF EXISTS breathing_exercises CASCADE;
DROP TABLE IF EXISTS meditation_topics CASCADE;
DROP TABLE IF EXISTS journal_prompts CASCADE;
DROP TABLE IF EXISTS ambient_sounds CASCADE;
DROP TABLE IF EXISTS articles CASCADE;

-- Audio CDN registry: maps mobile content IDs to CDN URLs
-- topic_id matches the string keys in the mobile's MeditationContent.kt
CREATE TABLE meditation_audio (
    topic_id   VARCHAR(100) PRIMARY KEY,
    audio_url  TEXT         NOT NULL,
    updated_at BIGINT       NOT NULL
);

-- sound_id matches the string keys in the mobile's MeditationContent.kt (ambientSounds)
CREATE TABLE ambient_sound_audio (
    sound_id   VARCHAR(100) PRIMARY KEY,
    audio_url  TEXT         NOT NULL,
    updated_at BIGINT       NOT NULL
);

-- Single favorites table for all content types.
-- content_type: 'affirmation' | 'breathing' | 'meditation' | 'journal_prompt'
-- content_id: the string key from the mobile (e.g. 'self_worth_worthy')
CREATE TABLE content_favorites (
    user_id      UUID         NOT NULL,
    content_type VARCHAR(50)  NOT NULL,
    content_id   VARCHAR(100) NOT NULL,
    created_at   BIGINT       NOT NULL,
    PRIMARY KEY (user_id, content_type, content_id)
);

CREATE INDEX idx_content_favorites_user_type ON content_favorites(user_id, content_type);
