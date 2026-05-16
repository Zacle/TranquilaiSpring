CREATE INDEX idx_mood_entries_user_created ON mood_entries(user_id, created_at DESC);
CREATE INDEX idx_journal_entries_user_created ON journal_entries(user_id, created_at DESC);
CREATE INDEX idx_breathing_sessions_user_created ON breathing_sessions(user_id, created_at DESC);
CREATE INDEX idx_meditation_sessions_user_created ON meditation_sessions(user_id, created_at DESC);
CREATE INDEX idx_journal_entries_user_category ON journal_entries(user_id, category, created_at DESC);
CREATE INDEX idx_journal_entries_user_favorite ON journal_entries(user_id, is_favorite);
