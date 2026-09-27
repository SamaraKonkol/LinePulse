CREATE TABLE organization_service_relationships (
    id UUID PRIMARY KEY,
    company_organization_id UUID NOT NULL REFERENCES organizations(id),
    provider_organization_id UUID NOT NULL REFERENCES organizations(id),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_service_relationship_company_provider UNIQUE (company_organization_id, provider_organization_id),
    CONSTRAINT chk_service_relationship_distinct_orgs CHECK (company_organization_id <> provider_organization_id)
);

CREATE TABLE service_requests (
    id UUID PRIMARY KEY,
    company_organization_id UUID NOT NULL REFERENCES organizations(id),
    provider_organization_id UUID REFERENCES organizations(id),
    machine_id UUID NOT NULL REFERENCES machines(id),
    incident_id UUID REFERENCES incidents(id),
    assigned_technician_id UUID REFERENCES users(id),
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    eta TIMESTAMPTZ,
    service_notes TEXT,
    parts_used TEXT,
    decline_reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at TIMESTAMPTZ,
    en_route_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    declined_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_service_request_provider_channel CHECK (
        (channel = 'INTERNAL' AND provider_organization_id IS NULL)
        OR (channel = 'EXTERNAL' AND provider_organization_id IS NOT NULL)
    )
);

CREATE INDEX idx_service_relationship_company ON organization_service_relationships(company_organization_id);
CREATE INDEX idx_service_relationship_provider ON organization_service_relationships(provider_organization_id);
CREATE INDEX idx_service_request_company ON service_requests(company_organization_id, requested_at DESC);
CREATE INDEX idx_service_request_provider ON service_requests(provider_organization_id, requested_at DESC);
CREATE INDEX idx_service_request_status ON service_requests(status);
CREATE INDEX idx_service_request_machine ON service_requests(machine_id);
CREATE INDEX idx_service_request_incident ON service_requests(incident_id);
CREATE INDEX idx_service_request_technician ON service_requests(assigned_technician_id);
