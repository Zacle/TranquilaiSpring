CREATE TABLE user_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    total_sessions INT NOT NULL DEFAULT 0,
    total_minutes INT NOT NULL DEFAULT 0,
    current_streak_days INT NOT NULL DEFAULT 0,
    longest_streak_days INT NOT NULL DEFAULT 0,
    mood_entries_count INT NOT NULL DEFAULT 0,
    journal_entries_count INT NOT NULL DEFAULT 0,
    plans_completed INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    badge_type VARCHAR(100) NOT NULL,
    badge_name VARCHAR(255) NOT NULL,
    description TEXT,
    awarded_at BIGINT NOT NULL,
    UNIQUE(user_id, badge_type)
);
