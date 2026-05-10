-- Meditation topic favorites
CREATE TABLE meditation_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    topic_id UUID NOT NULL REFERENCES meditation_topics(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    UNIQUE(user_id, topic_id)
);

-- Breathing exercise favorites
CREATE TABLE breathing_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    exercise_id UUID NOT NULL REFERENCES breathing_exercises(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    UNIQUE(user_id, exercise_id)
);

-- Articles (educational / wellness content)
CREATE TABLE articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT,
    category VARCHAR(100),
    image_url TEXT,
    read_time_minutes INT,
    language_code VARCHAR(10) NOT NULL DEFAULT 'en',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL
);

CREATE INDEX idx_meditation_favorites_user_id ON meditation_favorites(user_id);
CREATE INDEX idx_breathing_favorites_user_id ON breathing_favorites(user_id);
CREATE INDEX idx_articles_category ON articles(category);
CREATE INDEX idx_articles_language_code ON articles(language_code);
