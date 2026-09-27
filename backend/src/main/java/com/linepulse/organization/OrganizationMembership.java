package com.linepulse.organization;

import com.linepulse.auth.UserAccount;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "organization_memberships")
public class OrganizationMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    private OrganizationRole role;

    private boolean active;
    private Instant createdAt;

    protected OrganizationMembership() {
    }

    public OrganizationMembership(Organization organization, UserAccount user, OrganizationRole role, boolean active, Instant createdAt) {
        this.organization = organization;
        this.user = user;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public void changeRole(OrganizationRole role) {
        this.role = role;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public UserAccount getUser() { return user; }
    public OrganizationRole getRole() { return role; }
    public boolean isActive() { return active; }
}
