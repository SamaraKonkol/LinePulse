CREATE TABLE downtimes (
    id UUID PRIMARY KEY,
    machine_id UUID NOT NULL REFERENCES machines(id),
    incident_id UUID REFERENCES incidents(id),
    reason VARCHAR(240) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_downtimes_machine ON downtimes(machine_id);
CREATE INDEX idx_downtimes_incident ON downtimes(incident_id);
CREATE INDEX idx_downtimes_started_at ON downtimes(started_at);
