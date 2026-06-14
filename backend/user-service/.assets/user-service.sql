CREATE UNIQUE INDEX IF NOT EXISTS users_email_active_idx
ON users (email)
WHERE deleted_at IS NULL;
