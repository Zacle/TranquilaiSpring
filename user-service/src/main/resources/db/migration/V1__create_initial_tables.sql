-- User Service Schema

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth BIGINT,
    phone_number VARCHAR(20),
    timezone VARCHAR(50),
    language_preference VARCHAR(10) NOT NULL DEFAULT 'en',
    onboarding_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    profile_picture_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE mental_health_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Questionnaire answers (stored as comma-separated values)
    current_feeling_level VARCHAR(50),
    stress_causes TEXT,
    current_concerns TEXT,
    mental_process_preferences TEXT,
    personal_goals TEXT,
    identified_triggers TEXT,

    -- AI-generated insights
    personality_analysis TEXT,
    emotional_patterns TEXT,
    risk_factors TEXT,
    identified_strengths TEXT,
    recommended_approach TEXT,
    ai_coping_strategies TEXT,
    ai_focus_areas TEXT,

    -- AI assessment levels
    urgency_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    support_intensity VARCHAR(30) NOT NULL DEFAULT 'LIGHT',
    communication_style VARCHAR(20),

    -- Baseline metrics (1-10 scale)
    baseline_anxiety_level INT,
    baseline_depression_level INT,
    baseline_stress_level INT,
    baseline_wellbeing_level INT,
    baseline_coping_ability INT,

    -- Analysis metadata
    ai_analysis_version VARCHAR(20),
    ai_confidence_score DOUBLE PRECISION,
    last_ai_analysis_at BIGINT,

    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,

    CONSTRAINT unique_user_profile UNIQUE (user_id)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_mental_health_profiles_user_id ON mental_health_profiles(user_id);
