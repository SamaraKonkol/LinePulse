package com.linepulse.incident;

import com.linepulse.asset.Machine;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    private String rootCause;
    private String solution;
    private Instant occurredAt;
    private Instant resolvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected Incident() {
    }

    public Incident(UUID id, Machine machine, String title, String description, IncidentPriority priority, IncidentStatus status, Instant occurredAt, Instant createdAt, Instant updatedAt) {
        this(id, machine, title, description, IncidentCategory.OTHER, priority, status, occurredAt, createdAt, updatedAt);
    }

    public Incident(UUID id, Machine machine, String title, String description, IncidentCategory category, IncidentPriority priority, IncidentStatus status, Instant occurredAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.machine = machine;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void start(Instant at) {
        this.status = IncidentStatus.IN_PROGRESS;
        this.updatedAt = at;
    }

    public void resolve(String rootCause, String solution, Instant at) {
        this.status = IncidentStatus.RESOLVED;
        this.rootCause = rootCause;
        this.solution = solution;
        this.resolvedAt = at;
        this.updatedAt = at;
    }

    public void resolve(Instant at) {
        resolve("Não informado", "Não informado", at);
    }

    public void cancel(Instant at) {
        this.status = IncidentStatus.CANCELLED;
        this.updatedAt = at;
    }

    public UUID getId() { return id; }
    public Machine getMachine() { return machine; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentCategory getCategory() { return category; }
    public IncidentPriority getPriority() { return priority; }
    public IncidentStatus getStatus() { return status; }
    public String getRootCause() { return rootCause; }
    public String getSolution() { return solution; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
