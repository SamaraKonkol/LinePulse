ALTER TABLE users ADD COLUMN email VARCHAR(254);
ALTER TABLE users ADD COLUMN credentials_changed_at TIMESTAMPTZ;
CREATE UNIQUE INDEX uk_users_email ON users (LOWER(email)) WHERE email IS NOT NULL;
CREATE TABLE account_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    membership_id BIGINT REFERENCES organization_memberships(id) ON DELETE CASCADE,
    issued_by UUID REFERENCES users(id) ON DELETE SET NULL,
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('INVITE', 'RESET')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);
CREATE INDEX idx_account_tokens_user ON account_tokens(user_id);
