CREATE TABLE user_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

    -- Appearance
    theme_preference VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',  -- LIGHT | DARK | SYSTEM

    -- Notifications
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_time VARCHAR(10),          -- HH:mm, e.g. "08:00"
    reminder_frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY', -- DAILY | WEEKDAYS | WEEKENDS

    -- Content preferences
    preferred_content_language VARCHAR(10) NOT NULL DEFAULT 'en',
    show_explicit_content BOOLEAN NOT NULL DEFAULT FALSE,

    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX idx_user_settings_user_id ON user_settings(user_id);
