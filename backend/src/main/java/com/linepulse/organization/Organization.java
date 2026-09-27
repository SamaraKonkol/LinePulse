package com.linepulse.organization;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    private UUID id;

    private String name;
    private String slug;

    @Enumerated(EnumType.STRING)
    private OrganizationType type;

    private boolean active;
    private Instant createdAt;

    protected Organization() {
    }

    public Organization(UUID id, String name, String slug, OrganizationType type, boolean active, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.type = type;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public OrganizationType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }
}
