CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE organization_memberships (
    id BIGSERIAL PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_membership_org_user UNIQUE (organization_id, user_id)
);

INSERT INTO organizations (id, name, slug, type, active)
VALUES ('00000000-0000-0000-0000-000000000001', 'Empresa LinePulse', 'linepulse-default', 'COMPANY', TRUE);

INSERT INTO organization_memberships (organization_id, user_id, role, active)
SELECT
    '00000000-0000-0000-0000-000000000001',
    id,
    CASE role
        WHEN 'ADMIN' THEN 'OWNER'
        WHEN 'TECHNICIAN' THEN 'TECHNICIAN'
        ELSE 'OPERATOR'
    END,
    active
FROM users;

ALTER TABLE plants ADD COLUMN organization_id UUID;
UPDATE plants SET organization_id = '00000000-0000-0000-0000-000000000001';
ALTER TABLE plants ALTER COLUMN organization_id SET DEFAULT '00000000-0000-0000-0000-000000000001';
ALTER TABLE plants ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE plants ADD CONSTRAINT fk_plant_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE plants DROP CONSTRAINT IF EXISTS plants_code_key;
ALTER TABLE plants ADD CONSTRAINT uk_plant_org_code UNIQUE (organization_id, code);

CREATE INDEX idx_membership_user ON organization_memberships(user_id);
CREATE INDEX idx_membership_org ON organization_memberships(organization_id);
CREATE INDEX idx_plant_organization ON plants(organization_id);
