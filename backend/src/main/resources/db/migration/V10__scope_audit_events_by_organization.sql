ALTER TABLE audit_events ADD COLUMN organization_id UUID;

UPDATE audit_events
SET organization_id = (
    SELECT id
    FROM organizations
    WHERE slug = 'linepulse-default'
    LIMIT 1
)
WHERE organization_id IS NULL;

ALTER TABLE audit_events ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE audit_events
    ADD CONSTRAINT fk_audit_events_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

CREATE INDEX idx_audit_events_organization_created
    ON audit_events(organization_id, created_at DESC);
