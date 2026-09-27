ALTER TABLE incidents ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'OTHER';
ALTER TABLE incidents ADD COLUMN root_cause TEXT;
ALTER TABLE incidents ADD COLUMN solution TEXT;
ALTER TABLE incidents ADD COLUMN resolved_at TIMESTAMPTZ;

CREATE TABLE maintenance_plans (
    id UUID PRIMARY KEY,
    machine_id UUID NOT NULL REFERENCES machines(id),
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    interval_days INTEGER NOT NULL CHECK (interval_days > 0),
    next_due_date DATE NOT NULL,
    priority VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_generated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_plans_due ON maintenance_plans(active, next_due_date);
CREATE INDEX idx_incidents_category ON incidents(category);
