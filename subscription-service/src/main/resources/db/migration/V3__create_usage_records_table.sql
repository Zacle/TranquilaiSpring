CREATE TABLE IF NOT EXISTS usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    feature VARCHAR(100) NOT NULL,
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    count INTEGER NOT NULL DEFAULT 0,
    daily_limit INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usage_records_user_feature_date UNIQUE (user_id, feature, usage_date)
);

CREATE INDEX IF NOT EXISTS idx_usage_records_user_date ON usage_records(user_id, usage_date);
