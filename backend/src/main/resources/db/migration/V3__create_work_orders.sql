CREATE TABLE work_orders (
    id UUID PRIMARY KEY,
    machine_id UUID NOT NULL REFERENCES machines(id),
    incident_id UUID REFERENCES incidents(id),
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    scheduled_for TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_work_orders_machine ON work_orders(machine_id);
CREATE INDEX idx_work_orders_incident ON work_orders(incident_id);
CREATE INDEX idx_work_orders_status ON work_orders(status);
