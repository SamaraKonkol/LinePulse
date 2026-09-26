package com.linepulse.asset;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sectors")
public class Sector {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    private String name;
    private String code;
    private boolean active;
    private Instant createdAt;

    protected Sector() {
    }

    public Sector(UUID id, Plant plant, String name, String code, boolean active, Instant createdAt) {
        this.id = id;
        this.plant = plant;
        this.name = name;
        this.code = code;
        this.active = active;
        this.createdAt = createdAt;
    }

    public void update(Plant plant, String name, String code) {
        this.plant = plant;
        this.name = name;
        this.code = code;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public Plant getPlant() {
        return plant;
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
