CREATE TABLE mood_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    mood_score INT NOT NULL CHECK (mood_score BETWEEN 1 AND 10),
    mood_label VARCHAR(50),
    notes TEXT,
    factors TEXT,
    created_at BIGINT NOT NULL
);

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    category VARCHAR(50),
    prompt_id UUID,
    mood_score INT,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    ai_summary TEXT,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE breathing_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    duration_seconds INT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
);

CREATE TABLE meditation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    topic_id UUID NOT NULL,
    duration_minutes INT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL
);
