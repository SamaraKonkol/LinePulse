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
@Table(name = "production_lines")
public class ProductionLine {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    private String name;
    private String code;
    private boolean active;
    private Instant createdAt;

    protected ProductionLine() {
    }

    public ProductionLine(UUID id, Sector sector, String name, String code, boolean active, Instant createdAt) {
        this.id = id;
        this.sector = sector;
        this.name = name;
        this.code = code;
        this.active = active;
        this.createdAt = createdAt;
    }

    public void update(Sector sector, String name, String code) {
        this.sector = sector;
        this.name = name;
        this.code = code;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public Sector getSector() {
        return sector;
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
