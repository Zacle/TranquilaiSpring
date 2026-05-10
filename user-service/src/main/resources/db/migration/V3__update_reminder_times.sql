-- Replace single reminder_time column with reminder_times (supports multiple HH:mm values, comma-separated).
ALTER TABLE user_settings RENAME COLUMN reminder_time TO reminder_times;
ALTER TABLE user_settings ALTER COLUMN reminder_times TYPE TEXT;
