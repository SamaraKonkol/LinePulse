package com.linepulse.downtime;

import com.linepulse.asset.Machine;
import com.linepulse.incident.Incident;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "downtimes")
public class Downtime {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    private String reason;
    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;

    protected Downtime() {
    }

    public Downtime(UUID id, Machine machine, Incident incident, String reason, Instant startedAt, Instant createdAt) {
        this.id = id;
        this.machine = machine;
        this.incident = incident;
        this.reason = reason;
        this.startedAt = startedAt;
        this.createdAt = createdAt;
    }

    public void close(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public UUID getId() {
        return id;
    }

    public Machine getMachine() {
        return machine;
    }

    public Incident getIncident() {
        return incident;
    }

    public String getReason() {
        return reason;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
