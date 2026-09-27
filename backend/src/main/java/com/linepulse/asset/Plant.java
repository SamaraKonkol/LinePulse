package com.linepulse.asset;

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
@Table(name = "plants")
public class Plant {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String name;
    private String code;
    private boolean active;
    private Instant createdAt;

    protected Plant() {
    }

    public Plant(UUID id, Organization organization, String name, String code, boolean active, Instant createdAt) {
        this.id = id;
        this.organization = organization;
        this.name = name;
        this.code = code;
        this.active = active;
        this.createdAt = createdAt;
    }

    public void update(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
