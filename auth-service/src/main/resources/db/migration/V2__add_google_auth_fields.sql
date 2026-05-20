ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'PASSWORD',
    ADD COLUMN google_subject VARCHAR(255);

CREATE UNIQUE INDEX idx_users_google_subject
    ON users(google_subject)
    WHERE google_subject IS NOT NULL;

