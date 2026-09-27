package com.linepulse.audit;

import com.linepulse.organization.Organization;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String action;
    private String entityType;
    private UUID entityId;
    private String description;
    private String actorRegistration;
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, Organization organization, String action, String entityType, UUID entityId, String description, String actorRegistration, Instant createdAt) {
        this.id = id;
        this.organization = organization;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
        this.actorRegistration = actorRegistration;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getDescription() { return description; }
    public String getActorRegistration() { return actorRegistration; }
    public Instant getCreatedAt() { return createdAt; }
}
