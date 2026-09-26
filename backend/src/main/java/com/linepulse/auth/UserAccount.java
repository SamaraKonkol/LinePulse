package com.linepulse.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    private UUID id;

    private String name;
    private String registration;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    protected UserAccount() {
    }

    public UserAccount(UUID id, String name, String registration, String passwordHash, UserRole role, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.registration = registration;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void synchronizeDemoProfile(String name, String passwordHash, UserRole role, Instant updatedAt) {
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.updatedAt = updatedAt;
    }

    public void changeRole(UserRole role) {
        this.role = role;
        this.updatedAt = Instant.now();
    }

    public void changeActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegistration() {
        return registration;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
