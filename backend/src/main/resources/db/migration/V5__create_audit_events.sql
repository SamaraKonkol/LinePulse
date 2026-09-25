CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    description VARCHAR(255) NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_events_created_at ON audit_events(created_at DESC);
