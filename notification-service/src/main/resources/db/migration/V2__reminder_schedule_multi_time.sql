-- Allow multiple reminder times per user (one row per time slot).
-- Previously there was a UNIQUE constraint on user_id alone.

-- Drop the old single-user constraint and add composite uniqueness
ALTER TABLE reminder_schedules DROP CONSTRAINT IF EXISTS reminder_schedules_user_id_key;
ALTER TABLE reminder_schedules ADD CONSTRAINT reminder_schedules_user_id_time_key
    UNIQUE (user_id, reminder_time);
