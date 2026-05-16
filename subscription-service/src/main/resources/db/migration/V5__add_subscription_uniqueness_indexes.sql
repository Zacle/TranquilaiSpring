CREATE INDEX IF NOT EXISTS idx_subscriptions_purchase_token ON subscriptions(google_play_purchase_token);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_entitlements_user_feature'
    ) THEN
        ALTER TABLE entitlements ADD CONSTRAINT uq_entitlements_user_feature UNIQUE (user_id, feature);
    END IF;
END $$;
