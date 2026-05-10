CREATE TABLE daily_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    plan_date BIGINT NOT NULL,
    greeting TEXT NOT NULL,
    motivational_message TEXT NOT NULL,
    total_duration_minutes INT NOT NULL DEFAULT 0,
    completed_activities INT NOT NULL DEFAULT 0,
    is_fully_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ai_generated_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE(user_id, plan_date)
);

CREATE TABLE plan_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES daily_plans(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    content_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at BIGINT,
    created_at BIGINT NOT NULL
);
