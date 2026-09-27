package com.linepulse.service;

import com.linepulse.organization.Organization;
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
@Table(name = "organization_service_relationships")
public class ServiceRelationship {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_organization_id")
    private Organization company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_organization_id")
    private Organization provider;

    @Enumerated(EnumType.STRING)
    private ServiceRelationshipStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    protected ServiceRelationship() {
    }

    public ServiceRelationship(UUID id, Organization company, Organization provider, ServiceRelationshipStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.company = company;
        this.provider = provider;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void activate(Instant at) {
        this.status = ServiceRelationshipStatus.ACTIVE;
        this.updatedAt = at;
    }

    public void suspend(Instant at) {
        this.status = ServiceRelationshipStatus.SUSPENDED;
        this.updatedAt = at;
    }

    public UUID getId() { return id; }
    public Organization getCompany() { return company; }
    public Organization getProvider() { return provider; }
    public ServiceRelationshipStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
