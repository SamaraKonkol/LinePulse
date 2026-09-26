package com.linepulse.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;
    private String action;
    private String entityType;
    private UUID entityId;
    private String description;
    private String actorRegistration;
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, String action, String entityType, UUID entityId, String description, String actorRegistration, Instant createdAt) {
        this.id = id;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.actorRegistration = actorRegistration;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getDescription() { return description; }
    public String getActorRegistration() { return actorRegistration; }
    public Instant getCreatedAt() { return createdAt; }
}
