-- FCM device tokens per user
CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    device_name VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE(token)
);

-- Mirror of user reminder settings, kept in sync by user-service
CREATE TABLE reminder_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    reminder_time VARCHAR(5) NOT NULL,       -- HH:mm, e.g. "08:00"
    frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY', -- DAILY | WEEKDAYS | WEEKENDS
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at BIGINT NOT NULL
);

-- Log of every push notification sent (or attempted)
CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,  -- DAILY_REMINDER | STREAK_ALERT | PLAN_READY | CUSTOM
    status VARCHAR(20) NOT NULL,             -- SENT | FAILED | SKIPPED
    error_message TEXT,
    device_token TEXT,
    sent_at BIGINT NOT NULL
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX idx_reminder_schedules_time ON reminder_schedules(reminder_time, enabled);
CREATE INDEX idx_notification_logs_user_id ON notification_logs(user_id);
CREATE INDEX idx_notification_logs_sent_at ON notification_logs(sent_at);
