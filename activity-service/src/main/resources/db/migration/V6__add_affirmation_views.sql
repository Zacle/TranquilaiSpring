CREATE TABLE affirmation_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    affirmation_id VARCHAR(255) NOT NULL,
    viewed_at BIGINT NOT NULL
);

CREATE INDEX idx_affirmation_views_user_id ON affirmation_views(user_id);
