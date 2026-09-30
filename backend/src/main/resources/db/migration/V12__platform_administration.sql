ALTER TABLE users ADD COLUMN platform_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN platform_totp_counter BIGINT NOT NULL DEFAULT -1;

CREATE TABLE platform_audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_registration VARCHAR(40) NOT NULL,
    organization_id UUID REFERENCES organizations(id),
    action VARCHAR(80) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_platform_audit_created ON platform_audit_events(created_at DESC);
